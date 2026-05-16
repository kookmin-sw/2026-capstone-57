"use client"

import { useEffect, useRef, useCallback, useState } from "react"
import { Client, IMessage } from "@stomp/stompjs"
import SockJS from "sockjs-client"
import type { ChatMessage } from "@/types/match"

interface UseChatSocketOptions {
  sessionId: string | null
  onMessage?: (message: ChatMessage) => void
}

export function useChatSocket({ sessionId, onMessage }: UseChatSocketOptions) {
  const clientRef = useRef<Client | null>(null)
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    if (!sessionId) return

    const token = localStorage.getItem("token") || ""
    const userId = localStorage.getItem("userId") || ""

    const client = new Client({
      webSocketFactory: () => new SockJS("/ws/chat"),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true)

        // Subscribe to chat topic
        client.subscribe(`/topic/chat/${sessionId}`, (message: IMessage) => {
          try {
            const data = JSON.parse(message.body)
            const chatMsg: ChatMessage = {
              id: data.messageId,
              senderId: data.senderId,
              content: data.content,
              timestamp: data.createdAt
                ? new Date(data.createdAt).toLocaleTimeString("ko-KR", {
                    hour: "2-digit",
                    minute: "2-digit",
                  })
                : new Date().toLocaleTimeString("ko-KR", {
                    hour: "2-digit",
                    minute: "2-digit",
                  }),
              isMe: data.senderId === userId,
            }
            onMessage?.(chatMsg)
          } catch (e) {
            console.error("[ChatSocket] Failed to parse message:", e)
          }
        })
      },
      onDisconnect: () => {
        setConnected(false)
      },
      onStompError: (frame) => {
        console.error("[ChatSocket] STOMP error:", frame.headers?.["message"])
        setConnected(false)
      },
    })

    client.activate()
    clientRef.current = client

    return () => {
      client.deactivate()
      clientRef.current = null
      setConnected(false)
    }
  }, [sessionId])

  const sendMessage = useCallback(
    (content: string) => {
      if (!clientRef.current?.connected || !sessionId) return

      clientRef.current.publish({
        destination: `/app/chat/${sessionId}/send`,
        body: JSON.stringify({ content }),
      })
    },
    [sessionId]
  )

  return { connected, sendMessage }
}
