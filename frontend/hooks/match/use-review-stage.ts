"use client"

import { useState, useCallback } from "react"
import {
  getReview,
  selectReviewMode,
  submitReviewAnswer,
  generateReview,
  confirmReview,
  submitDirectReview,
  type ReviewMode,
  type ConversationTurn,
  type ReviewResultDto,
} from "@/lib/api/review"
import { toast } from "@/hooks/use-toast"

// ─── Types ───────────────────────────────────────────────────────────────────

export type ReviewPhase =
  | "loading"
  | "completed"
  | "mode-select"
  | "ai-conversation"
  | "ai-generating"
  | "ai-confirm"
  | "direct-write"

export interface UseReviewStageReturn {
  phase: ReviewPhase
  sessionId: string | null
  currentQuestion: string
  conversationHistory: ConversationTurn[]
  currentTurn: number
  maxTurns: number
  isConversationComplete: boolean
  generatedContent: string
  reflection: string
  setReflection: (v: string) => void
  rating: number
  setRating: (v: number) => void
  wantToMeetAgain: boolean
  setWantToMeetAgain: (v: boolean) => void
  completedReview: ReviewResultDto | null
  isLoading: boolean
  isSubmitting: boolean
  error: string | null

  // Actions
  loadReviewData: () => Promise<void>
  handleModeSelect: (mode: "ai" | "free") => Promise<void>
  handleSubmitAnswer: (answer: string) => Promise<void>
  handleGenerate: () => Promise<void>
  handleConfirmReview: () => Promise<void>
  handleDirectSubmit: () => Promise<void>
}

interface UseReviewStageOptions {
  matchId: string
  onComplete?: () => void
}

export function useReviewStage({
  matchId,
  onComplete,
}: UseReviewStageOptions): UseReviewStageReturn {
  const [phase, setPhase] = useState<ReviewPhase>("loading")
  const [sessionId, setSessionId] = useState<string | null>(null)
  const [currentQuestion, setCurrentQuestion] = useState("")
  const [conversationHistory, setConversationHistory] = useState<ConversationTurn[]>([])
  const [currentTurn, setCurrentTurn] = useState(1)
  const [maxTurns, setMaxTurns] = useState(5)
  const [isConversationComplete, setIsConversationComplete] = useState(false)
  const [generatedContent, setGeneratedContent] = useState("")
  const [reflection, setReflection] = useState("")
  const [rating, setRating] = useState(0)
  const [wantToMeetAgain, setWantToMeetAgain] = useState(true)
  const [completedReview, setCompletedReview] = useState<ReviewResultDto | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // ─── 초기 로딩: 기존 회고 조회 ─────────────────────────────────────────────

  const loadReviewData = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const existing = await getReview(matchId)
      if (existing) {
        setCompletedReview(existing)
        setPhase("completed")
      } else {
        setPhase("mode-select")
      }
    } catch {
      // 204 또는 404 → 미작성
      setPhase("mode-select")
    } finally {
      setIsLoading(false)
    }
  }, [matchId])

  // ─── 모드 선택 ─────────────────────────────────────────────────────────────

  const handleModeSelect = useCallback(async (mode: "ai" | "free") => {
    setIsSubmitting(true)
    setError(null)
    const apiMode: ReviewMode = mode === "ai" ? "AI_ASSISTED" : "DIRECT"

    try {
      const res = await selectReviewMode(matchId, apiMode)
      setSessionId(res.sessionId)
      setMaxTurns(res.maxTurns)
      setConversationHistory(res.conversationHistory || [])

      if (mode === "ai") {
        setCurrentQuestion(res.currentQuestion || "")
        setCurrentTurn(1)
        setPhase("ai-conversation")
      } else {
        setPhase("direct-write")
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "모드 선택에 실패했어요"
      setError(message)
      toast({ title: "오류", description: message, variant: "destructive" })
    } finally {
      setIsSubmitting(false)
    }
  }, [matchId])

  // ─── 답변 제출 (멀티턴) ────────────────────────────────────────────────────

  const handleSubmitAnswer = useCallback(async (answer: string) => {
    if (!sessionId || isSubmitting) return
    if (!answer.trim()) return

    setIsSubmitting(true)
    setError(null)

    try {
      const res = await submitReviewAnswer(matchId, sessionId, answer)

      setConversationHistory(res.conversationHistory)
      setCurrentTurn(res.currentTurn)
      setMaxTurns(res.maxTurns)
      setIsConversationComplete(res.isConversationComplete)

      if (res.isConversationComplete) {
        // 대화 완료 → 생성 단계로 전환
        setCurrentQuestion("")
        setPhase("ai-generating")
        await doGenerate(sessionId)
      } else {
        // 다음 질문 표시
        setCurrentQuestion(res.question || "")
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "답변 제출에 실패했어요"
      setError(message)
      toast({ title: "오류", description: message, variant: "destructive" })
    } finally {
      setIsSubmitting(false)
    }
  }, [sessionId, matchId, isSubmitting])

  // ─── AI 회고 생성 ──────────────────────────────────────────────────────────

  const doGenerate = async (sid: string) => {
    setIsLoading(true)
    setError(null)
    try {
      const res = await generateReview(matchId, sid)
      setGeneratedContent(res.generatedContent)
      setReflection(res.generatedContent)
      setRating(res.suggestedSatisfaction)
      setPhase("ai-confirm")
    } catch (err) {
      const message = err instanceof Error ? err.message : "회고 생성에 실패했어요"
      setError(message)
      toast({ title: "오류", description: message, variant: "destructive" })
      // 생성 실패 시 대화 단계로 복귀
      setPhase("ai-conversation")
    } finally {
      setIsLoading(false)
    }
  }

  const handleGenerate = useCallback(async () => {
    if (!sessionId) return
    setPhase("ai-generating")
    await doGenerate(sessionId)
  }, [sessionId, matchId])

  // ─── AI 회고 확정 ──────────────────────────────────────────────────────────

  const handleConfirmReview = useCallback(async () => {
    if (!sessionId || isSubmitting) return
    if (!reflection.trim() || rating === 0) return

    setIsSubmitting(true)
    setError(null)

    try {
      await confirmReview(matchId, sessionId, {
        reflection,
        satisfaction: rating,
        wantToMeetAgain,
      })
      setPhase("completed")
      setCompletedReview({
        sessionId,
        mode: "AI_ASSISTED",
        status: "COMPLETED",
        reflection,
        satisfaction: rating,
        wantToMeetAgain,
        createdAt: new Date().toISOString(),
      })
      toast({ title: "회고 저장 완료", description: "+50 XP 획득!" })
      onComplete?.()
    } catch (err) {
      const message = err instanceof Error ? err.message : "회고 저장에 실패했어요"
      setError(message)
      toast({ title: "오류", description: message, variant: "destructive" })
    } finally {
      setIsSubmitting(false)
    }
  }, [sessionId, reflection, rating, wantToMeetAgain, matchId, isSubmitting, onComplete])

  // ─── 직접 작성 제출 ────────────────────────────────────────────────────────

  const handleDirectSubmit = useCallback(async () => {
    if (isSubmitting) return
    if (!reflection.trim() || rating === 0) return

    setIsSubmitting(true)
    setError(null)

    try {
      await submitDirectReview(matchId, {
        satisfaction: rating,
        reflection,
        wantToMeetAgain,
      })
      setPhase("completed")
      setCompletedReview({
        sessionId: sessionId || "",
        mode: "DIRECT",
        status: "COMPLETED",
        reflection,
        satisfaction: rating,
        wantToMeetAgain,
        createdAt: new Date().toISOString(),
      })
      toast({ title: "회고 저장 완료", description: "+50 XP 획득!" })
      onComplete?.()
    } catch (err) {
      const message = err instanceof Error ? err.message : "회고 저장에 실패했어요"
      setError(message)
      toast({ title: "오류", description: message, variant: "destructive" })
    } finally {
      setIsSubmitting(false)
    }
  }, [reflection, rating, wantToMeetAgain, matchId, sessionId, isSubmitting, onComplete])

  return {
    phase,
    sessionId,
    currentQuestion,
    conversationHistory,
    currentTurn,
    maxTurns,
    isConversationComplete,
    generatedContent,
    reflection,
    setReflection,
    rating,
    setRating,
    wantToMeetAgain,
    setWantToMeetAgain,
    completedReview,
    isLoading,
    isSubmitting,
    error,

    loadReviewData,
    handleModeSelect,
    handleSubmitAnswer,
    handleGenerate,
    handleConfirmReview,
    handleDirectSubmit,
  }
}
