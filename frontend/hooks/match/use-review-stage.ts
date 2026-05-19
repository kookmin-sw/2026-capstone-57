"use client"

import { useState, useCallback } from "react"
import {
  getReview,
  selectReviewMode,
  getReviewQuestions,
  answerReviewQuestion,
  generateReview,
  confirmReview,
  submitDirectReview,
  type ReviewSessionDto,
  type ReviewQuestionDto,
  type ReviewResultDto,
  type ReviewMode,
} from "@/lib/api/review"
import { toast } from "@/hooks/use-toast"

// ─── Types ───────────────────────────────────────────────────────────────────

type ReviewPhase =
  | "loading"        // 초기 로딩
  | "completed"      // 이미 작성 완료 (readonly)
  | "mode-select"    // 모드 선택 화면
  | "ai-questions"   // AI 질문 답변 중
  | "ai-generating"  // AI 회고 생성 중
  | "ai-confirm"     // AI 생성 결과 확인/수정
  | "direct-write"   // 직접 작성

export interface UseReviewStageReturn {
  phase: ReviewPhase
  session: ReviewSessionDto | null
  questions: ReviewQuestionDto[]
  currentQuestionIndex: number
  generatedContent: string
  setGeneratedContent: (v: string) => void
  rating: number
  setRating: (v: number) => void
  wantToMeetAgain: boolean
  setWantToMeetAgain: (v: boolean) => void
  reflection: string
  setReflection: (v: string) => void
  completedReview: ReviewResultDto | null
  isLoading: boolean
  isSubmitting: boolean
  error: string | null

  // Actions
  loadReviewData: () => Promise<void>
  handleModeSelect: (mode: "ai" | "free") => Promise<void>
  handleAnswerQuestion: (answer: string) => Promise<void>
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
  const [session, setSession] = useState<ReviewSessionDto | null>(null)
  const [questions, setQuestions] = useState<ReviewQuestionDto[]>([])
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0)
  const [generatedContent, setGeneratedContent] = useState("")
  const [rating, setRating] = useState(0)
  const [wantToMeetAgain, setWantToMeetAgain] = useState(true)
  const [reflection, setReflection] = useState("")
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
    } catch (err) {
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
      const sessionData = await selectReviewMode(matchId, apiMode)
      setSession(sessionData)

      if (mode === "ai") {
        // AI 모드: 질문 조회
        const qs = await getReviewQuestions(matchId, sessionData.sessionId)
        const sorted = [...qs].sort((a, b) => a.questionOrder - b.questionOrder)
        setQuestions(sorted)

        // 이미 답변한 질문 건너뛰기
        const firstUnanswered = sorted.findIndex((q) => !q.answer)
        setCurrentQuestionIndex(firstUnanswered >= 0 ? firstUnanswered : sorted.length)

        // 모든 질문에 이미 답변했으면 생성 단계로
        if (firstUnanswered < 0) {
          setPhase("ai-generating")
          await handleGenerate(sessionData.sessionId)
        } else {
          setPhase("ai-questions")
        }
      } else {
        // 직접 작성 모드
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

  // ─── AI 질문 답변 ──────────────────────────────────────────────────────────

  const handleAnswerQuestion = useCallback(async (answer: string) => {
    if (!session || isSubmitting) return
    if (!answer.trim()) return

    const question = questions[currentQuestionIndex]
    if (!question) return

    setIsSubmitting(true)
    setError(null)

    try {
      await answerReviewQuestion(matchId, question.id, session.sessionId, answer)

      // 로컬 상태 업데이트
      setQuestions((prev) =>
        prev.map((q, i) => (i === currentQuestionIndex ? { ...q, answer } : q))
      )

      const nextIndex = currentQuestionIndex + 1

      if (nextIndex >= questions.length) {
        // 마지막 질문 → 회고 생성
        setPhase("ai-generating")
        await handleGenerate(session.sessionId)
      } else {
        setCurrentQuestionIndex(nextIndex)
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "답변 제출에 실패했어요"
      setError(message)
      toast({ title: "오류", description: message, variant: "destructive" })
    } finally {
      setIsSubmitting(false)
    }
  }, [session, questions, currentQuestionIndex, matchId, isSubmitting])

  // ─── AI 회고 생성 ──────────────────────────────────────────────────────────

  const handleGenerate = async (sessionId: string) => {
    setIsLoading(true)
    setError(null)
    try {
      const result = await generateReview(matchId, sessionId)
      setGeneratedContent(result.generatedContent)
      setReflection(result.generatedContent)
      setRating(result.suggestedSatisfaction)
      setPhase("ai-confirm")
    } catch (err) {
      const message = err instanceof Error ? err.message : "회고 생성에 실패했어요"
      setError(message)
      toast({ title: "오류", description: message, variant: "destructive" })
      // 생성 실패 시 질문 단계로 복귀
      setPhase("ai-questions")
    } finally {
      setIsLoading(false)
    }
  }

  // ─── AI 회고 확정 ──────────────────────────────────────────────────────────

  const handleConfirmReview = useCallback(async () => {
    if (!session || isSubmitting) return
    if (!reflection.trim() || rating === 0) return

    setIsSubmitting(true)
    setError(null)

    try {
      await confirmReview(matchId, session.sessionId, {
        reflection,
        satisfaction: rating,
        wantToMeetAgain,
      })
      setPhase("completed")
      setCompletedReview({
        sessionId: session.sessionId,
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
  }, [session, reflection, rating, wantToMeetAgain, matchId, isSubmitting, onComplete])

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
        sessionId: session?.sessionId || "",
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
  }, [reflection, rating, wantToMeetAgain, matchId, session, isSubmitting, onComplete])

  return {
    phase,
    session,
    questions,
    currentQuestionIndex,
    generatedContent,
    setGeneratedContent,
    rating,
    setRating,
    wantToMeetAgain,
    setWantToMeetAgain,
    reflection,
    setReflection,
    completedReview,
    isLoading,
    isSubmitting,
    error,

    loadReviewData,
    handleModeSelect,
    handleAnswerQuestion,
    handleConfirmReview,
    handleDirectSubmit,
  }
}
