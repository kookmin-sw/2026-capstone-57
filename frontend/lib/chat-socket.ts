import { Client, IMessage } from "@stomp/stompjs"
import SockJS from "sockjs-client"

// lib/chat-socket.ts
const WS_URL =
  process.env.NODE_ENV === "development"
    ? "http://98.93.112.251:8080/ws/chat"  // 개발: 직접 연결 (프록시 우회)
    : "/backend/ws/chat"                    // 프로덕션: Nginx 등 프록시 사용

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

export function connectChatSocket(
  sessionId: string,
  callbacks: ChatSocketCallbacks
): Client {
  // 기존 연결이 있으면 정리
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
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

      // 채팅 메시지 구독
      client.subscribe(`/topic/chat/${sessionId}`, (frame: IMessage) => {
        try {
          const data = JSON.parse(frame.body)

          // SessionEndedEvent 감지 (reason 필드가 있으면 종료 이벤트)
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

      // 세션 종료 전용 토픽 (별도 채널로 오는 경우 대비)
      client.subscribe(`/topic/chat/${sessionId}/end`, (frame: IMessage) => {
        try {
          const event = frame.body ? JSON.parse(frame.body) : undefined
          callbacks.onSessionEnd?.(event)
        } catch {
          callbacks.onSessionEnd?.()
        }
      })

      // 에러 큐 구독 (TOKEN_LIMIT_REACHED 등)
      client.subscribe(`/user/queue/errors`, (frame: IMessage) => {
        try {
          // 서버가 plain text 또는 JSON으로 에러를 보낼 수 있음
          let error: ChatErrorEvent

          try {
            error = JSON.parse(frame.body)
          } catch {
            // plain text인 경우 — 토큰 한도 메시지 등
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
      callbacks.onDisconnect?.()
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
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
}
