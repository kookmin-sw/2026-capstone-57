"use client"

import { useState, useEffect, useCallback } from "react"
import {
  getInteractionState,
  getQuiz,
  submitQuiz,
  getHints,
  sendHint,
  createChatSession,
  type InteractionStateDto,
  type HintQuestionDto,
  type ChatSessionDto,
} from "@/lib/api/interaction"
import { STAGE_MAP } from "./use-match-state"
import type { QuizQuestion } from "@/types/match"
import type { InteractionStage } from "@/types/slot"

export interface UseQuizStageReturn {
  quizQuestions: QuizQuestion[]
  quizHints: HintQuestionDto[]
  quizWaiting: boolean
  setQuizHints: React.Dispatch<React.SetStateAction<HintQuestionDto[]>>
  handleSubmitAnswer: (quizIndex: number, answer: number) => Promise<{ correctAnswer: number; isCorrect: boolean } | null>
  handleQuizComplete: () => Promise<void>
  loadQuizData: () => Promise<void>
  /** Called externally when transitioning to chat completes */
  transitionToChat: () => Promise<ChatSessionDto | null>
}

interface UseQuizStageOptions {
  matchId: string
  interaction: InteractionStateDto | null
  setInteraction: React.Dispatch<React.SetStateAction<InteractionStateDto | null>>
  setActiveStage: React.Dispatch<React.SetStateAction<InteractionStage>>
  onChatSessionCreated: (session: ChatSessionDto) => void
}

export function useQuizStage({
  matchId,
  interaction,
  setInteraction,
  setActiveStage,
  onChatSessionCreated,
}: UseQuizStageOptions): UseQuizStageReturn {
  const [quizQuestions, setQuizQuestions] = useState<QuizQuestion[]>([])
  const [quizHints, setQuizHints] = useState<HintQuestionDto[]>([])
  const [quizWaiting, setQuizWaiting] = useState(false)
  const isTransitioningRef = { current: false }

  // 이미 퀴즈 완료 후 대기 중인 경우 감지
  useEffect(() => {
    if (!interaction) return
    const userId = localStorage.getItem("userId") || ""
    const quizCompletedBy: string[] = interaction.stageData?.quizCompletedBy || []
    if (interaction.currentStage === 1 && interaction.stageStatus === "WAITING" && quizCompletedBy.includes(userId)) {
      setQuizWaiting(true)
    }
  }, [interaction])

  const loadQuizData = useCallback(async () => {
    try {
      const questions = await getQuiz(matchId)
      if (!Array.isArray(questions)) {
        console.warn("getQuiz 응답이 배열이 아님:", questions)
        return
      }
      setQuizQuestions(
        questions.map((q) => ({
          id: `q${q.quizIndex}`,
          question: q.question,
          options: q.options,
          correctIndex: -1,
        }))
      )
      try {
        const hints = await getHints(matchId)
        setQuizHints(hints)
      } catch {
        setQuizHints([])
      }
    } catch (err) {
      console.error("퀴즈 데이터 로드 실패:", err)
    }
  }, [matchId])

  /** 채팅 단계로 전환: 세션 생성 → 콜백 호출 */
  const transitionToChat = useCallback(async (): Promise<ChatSessionDto | null> => {
    if (isTransitioningRef.current) return null
    isTransitioningRef.current = true

    try {
      // 1. 채팅 세션 생성
      const session = await createChatSession(matchId)

      // 2. 단계 전환
      setActiveStage("CHAT")
      setQuizWaiting(false)

      // 3. 상호작용 상태 갱신
      const state = await getInteractionState(matchId)
      setInteraction(state)

      // 4. 콜백으로 세션 전달
      onChatSessionCreated(session)

      return session
    } catch (err) {
      console.error("채팅 세션 생성 실패:", err)
      isTransitioningRef.current = false
      return null
    }
  }, [matchId, setActiveStage, setInteraction, onChatSessionCreated])

  /** 퀴즈 답안 제출 핸들러 */
  const handleSubmitAnswer = useCallback(async (quizIndex: number, answer: number) => {
    try {
      const result = await submitQuiz(matchId, {
        quizIndex: quizIndex + 1, // 1-based
        answer,
      })

      if (result.allCompleted) {
        // 내 퀴즈 모두 완료 → 상대방 완료 여부 확인
        const state = await getInteractionState(matchId)
        setInteraction(state)

        if (state.currentStage >= 2 && state.stageStatus !== "WAITING") {
          // 양쪽 모두 완료 → 즉시 채팅 전환
          setQuizWaiting(false)
          await transitionToChat()
        }
      }

      return { correctAnswer: result.correctAnswer, isCorrect: result.isCorrect }
    } catch (err) {
      console.error("퀴즈 제출 실패:", err)
      return null
    }
  }, [matchId, setInteraction, transitionToChat])

  /** 퀴즈 완료 핸들러 (마지막 문항 "완료" 버튼) */
  const handleQuizComplete = useCallback(async () => {
    setQuizWaiting(true)
  }, [])

  // 대기 중일 때 폴링으로 상태 확인
  useEffect(() => {
    if (!quizWaiting) return

    const interval = setInterval(async () => {
      try {
        const state = await getInteractionState(matchId)
        setInteraction(state)

        if (state.currentStage >= 2 && state.stageStatus !== "WAITING") {
          setQuizWaiting(false)
          await transitionToChat()
        }
      } catch (err) {
        console.error("상태 폴링 실패:", err)
      }
    }, 5000)

    return () => clearInterval(interval)
  }, [quizWaiting, matchId, setInteraction, transitionToChat])

  return {
    quizQuestions,
    quizHints,
    quizWaiting,
    setQuizHints,
    handleSubmitAnswer,
    handleQuizComplete,
    loadQuizData,
    transitionToChat,
  }
}
