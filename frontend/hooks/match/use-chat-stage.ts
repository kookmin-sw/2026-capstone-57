"use client"

import { useState, useCallback } from "react"
import {
  getInteractionState,
  getChatSession,
  getChatMessages,
  type ChatSessionDto,
  type InteractionStateDto,
} from "@/lib/api/interaction"
import {
  connectChatSocket,
  sendChatMessage,
  disconnectChatSocket,
  type IncomingChatMessage,
} from "@/lib/chat-socket"
import { STAGE_MAP } from "./use-match-state"
import type { ChatMessage } from "@/types/match"
import type { InteractionStage } from "@/types/slot"

export interface UseChatStageReturn {
  chatMessages: ChatMessage[]
  chatSession: ChatSessionDto | null
  usedTokens: number
  chatEnded: boolean
  isStompConnected: boolean
  setChatSession: React.Dispatch<React.SetStateAction<ChatSessionDto | null>>
  handleSendMessage: (content: string) => void
  initChatSocket: (sessionId: string) => void
  loadChatData: () => Promise<void>
  /** Set up chat from a freshly created session (used by quiz→chat transition) */
  setupFromSession: (session: ChatSessionDto) => void
}

interface UseChatStageOptions {
  matchId: string
  setInteraction: React.Dispatch<React.SetStateAction<InteractionStateDto | null>>
  setActiveStage: React.Dispatch<React.SetStateAction<InteractionStage>>
}

export function useChatStage({
  matchId,
  setInteraction,
  setActiveStage,
}: UseChatStageOptions): UseChatStageReturn {
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([])
  const [chatSession, setChatSession] = useState<ChatSessionDto | null>(null)
  const [usedTokens, setUsedTokens] = useState(0)
  const [chatEnded, setChatEnded] = useState(false)
  const [isStompConnected, setIsStompConnected] = useState(false)

  /** 채팅 단계 완료 → 다음 단계로 전환 */
  const handleChatCompleted = useCallback(async () => {
    try {
      const state = await getInteractionState(matchId)
      setInteraction(state)
      const nextStage = STAGE_MAP[state.currentStage] || "GAME"
      setActiveStage(nextStage)
    } catch (err) {
      console.error("채팅 완료 후 상태 갱신 실패:", err)
      // fallback: 게임 단계로 전환
      setActiveStage("GAME")
    }
  }, [matchId, setInteraction, setActiveStage])

  /** WebSocket(STOMP) 연결 초기화 */
  const initChatSocket = useCallback((sessionId: string) => {
    const userId = localStorage.getItem("userId") || ""

    connectChatSocket(sessionId, {
      onConnect: () => {
        setIsStompConnected(true)
      },
      onDisconnect: () => {
        setIsStompConnected(false)
      },
      onMessage: (msg: IncomingChatMessage) => {
        const chatMsg: ChatMessage = {
          id: msg.messageId,
          senderId: msg.senderId,
          content: msg.content,
          timestamp: new Date(msg.createdAt).toLocaleTimeString("ko-KR", {
            hour: "2-digit",
            minute: "2-digit",
          }),
          isMe: msg.senderId === userId,
        }
        setChatMessages((prev) => [...prev, chatMsg])
      },
      onTokenUpdate: (tokens: number) => {
        setUsedTokens(tokens)
      },
      onSessionEnd: () => {
        setChatEnded(true)
        disconnectChatSocket()
        setIsStompConnected(false)
        // 다음 단계로 자동 전환
        handleChatCompleted()
      },
      onChatError: (error) => {
        console.warn("채팅 에러:", error.code, error.message)
      },
      onError: (err) => {
        console.error("채팅 소켓 에러:", err)
      },
    })
  }, [handleChatCompleted])

  /** 채팅 메시지 전송 */
  const handleSendMessage = useCallback((content: string) => {
    if (!chatSession || !isStompConnected) return
    sendChatMessage(chatSession.sessionId, content)
  }, [chatSession, isStompConnected])

  /** 기존 채팅 세션 데이터 로드 */
  const loadChatData = useCallback(async () => {
    try {
      const session = await getChatSession(matchId)
      setChatSession(session)
      setUsedTokens(session.usedTokens)
      if (session.status === "ENDED") {
        setChatEnded(true)
      }
      // 기존 메시지 로드
      const messages = await getChatMessages(session.sessionId)
      const userId = localStorage.getItem("userId") || ""
      setChatMessages(
        messages.map((m) => ({
          id: m.messageId,
          senderId: m.senderId,
          content: m.content,
          timestamp: new Date(m.createdAt).toLocaleTimeString("ko-KR", {
            hour: "2-digit",
            minute: "2-digit",
          }),
          isMe: m.senderId === userId,
        }))
      )
      // WebSocket 연결
      if (session.status === "ACTIVE") {
        initChatSocket(session.sessionId)
      }
    } catch {
      setChatMessages([])
    }
  }, [matchId, initChatSocket])

  /** 퀴즈→채팅 전환 시 새 세션으로 설정 */
  const setupFromSession = useCallback((session: ChatSessionDto) => {
    setChatSession(session)
    setUsedTokens(session.usedTokens)
    setChatEnded(false)
    setChatMessages([])
    initChatSocket(session.sessionId)
  }, [initChatSocket])

  return {
    chatMessages,
    chatSession,
    usedTokens,
    chatEnded,
    isStompConnected,
    setChatSession,
    handleSendMessage,
    initChatSocket,
    loadChatData,
    setupFromSession,
  }
}
