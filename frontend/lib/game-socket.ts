import { Client, IMessage } from "@stomp/stompjs"
import SockJS from "sockjs-client"

const WS_URL =
  process.env.NODE_ENV === "development"
    ? "http://98.93.112.251:8080/ws/game"
    : "/backend/ws/game"

export interface GameEvent {
  type: string
  [key: string]: unknown
}

export interface GameSocketCallbacks {
  onEvent: (event: GameEvent) => void
  onConnect?: () => void
  onDisconnect?: () => void
  onError?: (error: unknown) => void
}

let gameStompClient: Client | null = null

export function connectGameSocket(
  gameSessionId: string,
  callbacks: GameSocketCallbacks
): Client {
  if (gameStompClient) {
    gameStompClient.deactivate()
    gameStompClient = null
  }

  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null

  const client = new Client({
    webSocketFactory: () => new SockJS(WS_URL) as unknown as WebSocket,
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      callbacks.onConnect?.()

      // 게임 이벤트 구독
      client.subscribe(`/topic/game/${gameSessionId}`, (frame: IMessage) => {
        try {
          const event: GameEvent = JSON.parse(frame.body)
          console.log("[Game WS] 이벤트 수신:", event.type, event)
          callbacks.onEvent(event)
        } catch (err) {
          callbacks.onError?.(err)
        }
      })

      // READY 액션 전송 (client를 직접 사용하여 타이밍 이슈 방지)
      const readyPayload = JSON.stringify({ type: "READY" })
      console.log("[Game WS] READY 전송:", `/app/game/${gameSessionId}/action`)
      client.publish({
        destination: `/app/game/${gameSessionId}/action`,
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: readyPayload,
      })
    },
    onDisconnect: () => {
      callbacks.onDisconnect?.()
    },
    onStompError: (frame) => {
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
  if (gameStompClient) {
    gameStompClient.deactivate()
    gameStompClient = null
  }
}
