import { Client, IMessage } from "@stomp/stompjs"
import SockJS from "sockjs-client"

const WS_URL =
  typeof window !== "undefined" && window.location.hostname === "localhost"
    ? "http://98.93.112.251:8080/ws"
    : "/backend/ws"

export interface GameEvent {
  type: string
  [key: string]: unknown
}

export interface GameSocketCallbacks {
  onEvent: (event: GameEvent) => void
  onConnect?: () => void
  onDisconnect?: () => void
  onError?: (error: unknown) => void
  onGameError?: (message: string) => void
}

let gameStompClient: Client | null = null
let isGameConnecting = false

export function connectGameSocket(
  gameSessionId: string,
  callbacks: GameSocketCallbacks
): Client | null {
  // 방어: 필수 값 검증
  if (!gameSessionId) {
    console.error("[Game WS] gameSessionId is missing, cannot connect")
    return null
  }

  // 중복 연결 방지
  if (isGameConnecting) {
    console.warn("[Game WS] Already connecting, skipping")
    return gameStompClient
  }

  // 기존 연결 정리
  if (gameStompClient) {
    gameStompClient.deactivate()
    gameStompClient = null
  }

  isGameConnecting = true
  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null

  const client = new Client({
    webSocketFactory: () => new SockJS(WS_URL) as unknown as WebSocket,
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      isGameConnecting = false
      callbacks.onConnect?.()

      // 게임 이벤트 구독
      client.subscribe(`/topic/game/${gameSessionId}`, (frame: IMessage) => {
        try {
          const event: GameEvent = JSON.parse(frame.body)
          console.log("[Game WS] 이벤트 수신:", event.type)
          callbacks.onEvent(event)
        } catch (err) {
          callbacks.onError?.(err)
        }
      })

      // READY 전송
      console.log("[Game WS] READY 전송:", `/app/game/${gameSessionId}/action`)
      client.publish({
        destination: `/app/game/${gameSessionId}/action`,
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: JSON.stringify({ type: "READY" }),
      })

      // 에러 큐 구독
      client.subscribe(`/user/queue/errors`, (frame: IMessage) => {
        try {
          const body = frame.body || ""
          console.warn("[Game WS] 에러 수신:", body)
          callbacks.onGameError?.(body)
        } catch (err) {
          callbacks.onError?.(err)
        }
      })
    },
    onDisconnect: () => {
      isGameConnecting = false
      callbacks.onDisconnect?.()
    },
    onStompError: (frame) => {
      isGameConnecting = false
      console.error("Game STOMP error:", frame.headers["message"])
      callbacks.onError?.(frame)
    },
  })

  client.activate()
  gameStompClient = client
  return client
}

export function sendGameAction(gameSessionId: string, action: { type: string; [key: string]: unknown }) {
  if (!gameStompClient || !gameStompClient.connected) {
    console.error("Game STOMP client is not connected")
    return
  }

  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null

  gameStompClient.publish({
    destination: `/app/game/${gameSessionId}/action`,
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: JSON.stringify(action),
  })
}

export function disconnectGameSocket() {
  isGameConnecting = false
  if (gameStompClient) {
    gameStompClient.deactivate()
    gameStompClient = null
  }
}
