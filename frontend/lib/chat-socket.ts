import { Client, IMessage } from "@stomp/stompjs"
import SockJS from "sockjs-client"

const WS_URL =
  typeof window !== "undefined" && window.location.hostname === "localhost"
    ? "http://54.89.211.175:8080/ws"
    : "/backend/ws"

export interface IncomingChatMessage {
  messageId: string
  sessionId: string
  senderId: string
  content: string
  createdAt: string
  usedTokens: number
  tokenLimit: number
}

export interface SessionEndedEvent {
  sessionId: string
  reason: string
  endedAt?: string
}

export interface ChatErrorEvent {
  code: string
  message: string
  sessionId?: string
}

export interface ChatSocketCallbacks {
  onMessage: (msg: IncomingChatMessage) => void
  onTokenUpdate?: (usedTokens: number) => void
  onSessionEnd?: (event?: SessionEndedEvent) => void
  onError?: (error: unknown) => void
  onChatError?: (error: ChatErrorEvent) => void
  onConnect?: () => void
  onDisconnect?: () => void
}

let stompClient: Client | null = null
let isChatConnecting = false

export function connectChatSocket(
  sessionId: string,
  callbacks: ChatSocketCallbacks
): Client | null {
  // 방어: 필수 값 검증
  if (!sessionId) {
    console.error("[Chat WS] sessionId is missing, cannot connect")
    return null
  }

  // 중복 연결 방지
  if (isChatConnecting) {
    console.warn("[Chat WS] Already connecting, skipping")
    return stompClient
  }

  // 기존 연결 정리
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }

  isChatConnecting = true
  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null

  const client = new Client({
    webSocketFactory: () => new SockJS(WS_URL) as unknown as WebSocket,
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      isChatConnecting = false
      callbacks.onConnect?.()

      // 채팅 메시지 구독
      client.subscribe(`/topic/chat/${sessionId}`, (frame: IMessage) => {
        try {
          const data = JSON.parse(frame.body)

          // SessionEndedEvent 감지
          if (data.reason || data.type === "SESSION_ENDED") {
            callbacks.onSessionEnd?.(data as SessionEndedEvent)
            return
          }

          // 일반 채팅 메시지
          const msg = data as IncomingChatMessage
          callbacks.onMessage(msg)
          if (callbacks.onTokenUpdate) {
            callbacks.onTokenUpdate(msg.usedTokens)
          }
        } catch (err) {
          callbacks.onError?.(err)
        }
      })

      // 세션 종료 전용 토픽
      client.subscribe(`/topic/chat/${sessionId}/end`, (frame: IMessage) => {
        try {
          const event = frame.body ? JSON.parse(frame.body) : undefined
          callbacks.onSessionEnd?.(event)
        } catch {
          callbacks.onSessionEnd?.()
        }
      })

      // 에러 큐 구독
      client.subscribe(`/user/queue/errors`, (frame: IMessage) => {
        try {
          let error: ChatErrorEvent

          try {
            error = JSON.parse(frame.body)
          } catch {
            const body = frame.body || ""
            const isTokenLimit = body.includes("토큰") || body.includes("한도") || body.includes("TOKEN_LIMIT")
            error = {
              code: isTokenLimit ? "TOKEN_LIMIT_REACHED" : "UNKNOWN",
              message: body,
            }
          }

          callbacks.onChatError?.(error)

          if (error.code === "TOKEN_LIMIT_REACHED") {
            callbacks.onSessionEnd?.({ sessionId, reason: error.code })
          }
        } catch (err) {
          callbacks.onError?.(err)
        }
      })
    },
    onDisconnect: () => {
      isChatConnecting = false
      callbacks.onDisconnect?.()
    },
    onStompError: (frame) => {
      isChatConnecting = false
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
    console.error("STOMP client is not connected:", {
      exists: !!stompClient,
      connected: stompClient?.connected,
    })
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
  isChatConnecting = false
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
}
