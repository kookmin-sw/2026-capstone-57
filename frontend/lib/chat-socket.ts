import { Client, IMessage } from "@stomp/stompjs"
import SockJS from "sockjs-client"

const WS_URL = "/backend/ws"

export interface IncomingChatMessage {
  messageId: string
  sessionId: string
  senderId: string
  content: string
  createdAt: string
  usedTokens?: number
}

export interface ChatSocketCallbacks {
  onMessage: (msg: IncomingChatMessage) => void
  onTokenUpdate?: (usedTokens: number) => void
  onSessionEnd?: () => void
  onError?: (error: unknown) => void
}

let stompClient: Client | null = null

export function connectChatSocket(
  sessionId: string,
  callbacks: ChatSocketCallbacks
): Client {
  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null

  const client = new Client({
    webSocketFactory: () => new SockJS(WS_URL) as unknown as WebSocket,
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      // 채팅 메시지 구독
      client.subscribe(`/topic/chat/${sessionId}`, (frame: IMessage) => {
        try {
          const msg: IncomingChatMessage = JSON.parse(frame.body)
          callbacks.onMessage(msg)
          if (msg.usedTokens !== undefined && callbacks.onTokenUpdate) {
            callbacks.onTokenUpdate(msg.usedTokens)
          }
        } catch (err) {
          callbacks.onError?.(err)
        }
      })

      // 세션 종료 이벤트 구독
      client.subscribe(`/topic/chat/${sessionId}/end`, () => {
        callbacks.onSessionEnd?.()
      })
    },
    onStompError: (frame) => {
      console.error("STOMP error:", frame.headers["message"])
      callbacks.onError?.(frame)
    },
  })

  client.activate()
  stompClient = client
  return client
}

export function sendChatMessage(sessionId: string, content: string) {
  if (!stompClient || !stompClient.connected) {
    console.error("STOMP client is not connected")
    return
  }

  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null

  stompClient.publish({
    destination: `/app/chat/${sessionId}/send`,
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: JSON.stringify({ content }),
  })
}

export function disconnectChatSocket() {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
}
