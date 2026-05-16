import { Client, IMessage } from "@stomp/stompjs"
import SockJS from "sockjs-client"

const WS_URL =
  process.env.NODE_ENV === "development"
    ? "http://54.174.25.221:8080/ws/game"
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
          callbacks.onEvent(event)
        } catch (err) {
          callbacks.onError?.(err)
        }
      })

      // READY 액션 전송
      sendGameAction(gameSessionId, { type: "READY" })
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
