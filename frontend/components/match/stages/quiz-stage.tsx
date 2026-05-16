"use client"

import { useState, useEffect } from "react"
import { MessageCircle, Check, X, Send } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Drawer,
  DrawerContent,
  DrawerHeader,
  DrawerTitle,
  DrawerFooter,
} from "@/components/ui/drawer"
import { cn } from "@/lib/utils"
import { getHints } from "@/lib/api/interaction"
import type { QuizQuestion } from "@/types/match"

interface HintItem {
  id: string
  question: string
  answer: string | null
  status: "PENDING" | "ANSWERED"
  quizIndex: number
  senderId: string
}

interface QuizStageProps {
  questions: QuizQuestion[]
  hints?: HintItem[]
  currentUserId: string
  onComplete: () => void
  onSubmitAnswer?: (quizIndex: number, answer: number) => Promise<{ correctAnswer: number; isCorrect: boolean } | null>
  onSendHint?: (question: string, quizIndex: number) => void
  onHintsUpdate?: (hints: HintItem[]) => void
  matchId: string
  initialIndex?: number
}

export function QuizStage({ questions, hints = [], currentUserId, onComplete, onSubmitAnswer, onSendHint, onHintsUpdate, matchId, initialIndex }: QuizStageProps) {
  const STORAGE_KEY = `quiz_progress_${matchId}`

  const [currentIndex, setCurrentIndex] = useState(() => {
    if (initialIndex !== undefined) return initialIndex
    if (typeof window === "undefined") return 0
    const saved = localStorage.getItem(STORAGE_KEY)
    return saved ? parseInt(saved, 10) : 0
  })
  const [selectedOption, setSelectedOption] = useState<number | null>(null)
  const [correctAnswer, setCorrectAnswer] = useState<number | null>(null)
  const [showResult, setShowResult] = useState(false)
  const [showHintInput, setShowHintInput] = useState(false)
  const [hintInput, setHintInput] = useState("")

  const currentQuestion = questions[currentIndex]
  const isLastQuestion = currentIndex === questions.length - 1

  // 답변 대기 중인 힌트가 있을 때만 폴링
  useEffect(() => {
    const hasPending = hints.some((h) => !h.answer)
    if (!hasPending) return

    const timer = setInterval(async () => {
      try {
        const updated = await getHints(matchId)
        const mapped = updated.map((h) => ({
          id: h.id,
          question: h.question,
          answer: h.answer,
          status: h.status,
          quizIndex: h.quizIndex,
        }))
        onHintsUpdate?.(mapped)
      } catch (err) {
        console.error("힌트 업데이트 실패:", err)
      }
    }, 10000)

    return () => clearInterval(timer)
  }, [hints, matchId, onHintsUpdate])

  const handleSelect = async (optionIndex: number) => {
    if (showResult) return
    setSelectedOption(optionIndex)

    let resolvedCorrectAnswer = currentQuestion.correctIndex ?? 0

    if (onSubmitAnswer) {
      try {
        const result = await onSubmitAnswer(currentIndex, optionIndex)
        if (result !== null && result.correctAnswer !== undefined) {
          resolvedCorrectAnswer = result.correctAnswer
        }
      } catch (err) {
        console.error("퀴즈 제출 실패:", err)
      }
    } else {
      resolvedCorrectAnswer = currentQuestion.correctIndex
    }

    setCorrectAnswer(resolvedCorrectAnswer)  // correctAnswer 먼저
    setShowResult(true)                      // 그 다음 showResult
  }

  const handleNext = () => {
    if (isLastQuestion) {
      localStorage.removeItem(STORAGE_KEY)
      onComplete()
    } else {
      const nextIndex = currentIndex + 1
      localStorage.setItem(STORAGE_KEY, String(nextIndex))
      setCurrentIndex(nextIndex)
      setSelectedOption(null)
      setCorrectAnswer(null)
      setShowResult(false)
    }
  }

  const handleSendHint = () => {
    if (!hintInput.trim()) return
    onSendHint?.(hintInput.trim(), currentIndex + 1)
    setHintInput("")
    setShowHintInput(false)
  }

  if (!currentQuestion || currentQuestion.options.length === 0) {
    return (
      <div className="flex items-center justify-center bg-card rounded-2xl p-4 shadow-sm border border-border/30">
        <p className="text-sm text-muted-foreground">퀴즈를 불러오는 중...</p>
      </div>
    )
  }

  return (
    // ✅ h-full 제거 → 내용물 높이에 맞게 자연스럽게 늘어남
    <div className="flex flex-col bg-card rounded-2xl p-4 shadow-sm border border-border/30 pb-6">
      {/* Header */}
      <div className="flex justify-between items-center mb-2">
        <h3 className="text-sm font-semibold text-foreground">1단계 · 퀴즈</h3>
        <span className="text-[11px] text-muted-foreground">
          {currentIndex + 1} / {questions.length}
        </span>
      </div>

      {/* Question */}
      <p className="text-sm font-semibold text-foreground mb-3">
        {currentQuestion.question}
      </p>

      {/* Options */}
      {/* ✅ flex-1, justify-center, min-h-0, overflow-hidden 모두 제거 */}
      <div className="flex flex-col gap-2 mt-1">
        {currentQuestion.options.map((option, index) => {
          const isSelected = selectedOption === index
          const isCorrect = showResult && correctAnswer !== null && index === correctAnswer
          const isWrong = showResult && isSelected && !isCorrect

          return (
            <button
              key={index}
              type="button"
              onClick={() => handleSelect(index)}
              disabled={showResult}
              className={cn(
                "w-full py-2 px-3 rounded-xl text-left text-sm transition-all",
                "border bg-secondary/40",
                !showResult && "hover:bg-secondary/60 active:scale-[0.98]",
                isCorrect && "border-green-400 bg-green-50 text-green-700",
                isWrong && "border-destructive/60 bg-destructive/10 text-destructive"
              )}
            >
              <div className="flex items-center justify-between">
                <span>{option}</span>
                {isCorrect && (
                  <span className="flex items-center gap-1 text-green-600">
                    <Check className="w-4 h-4" />
                    <span className="text-[10px]">정답</span>
                  </span>
                )}
                {isWrong && <X className="w-4 h-4 text-destructive" />}
              </div>
            </button>
          )
        })}
      </div>

      {/* Result message */}
      {showResult && selectedOption !== null && correctAnswer !== null && (
        <div className={cn(
          "text-center text-xs py-1.5 rounded-lg mt-2",
          selectedOption === correctAnswer
            ? "bg-green-50 text-green-700"
            : "bg-destructive/10 text-destructive"
        )}>
          {selectedOption === correctAnswer ? "정답이에요! 🎉" : "아쉬워요, 정답을 확인해보세요"}
        </div>
      )}

      {/* Actions */}
      <div className="flex items-center justify-between gap-2 mt-3">
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={(e) => { e.preventDefault(); setShowHintInput(true) }}
          className="h-8 rounded-full bg-secondary/60 border-0 text-[11px]"
        >
          <MessageCircle className="w-3 h-3 mr-1" />
          힌트 질문
        </Button>

        <Button
          type="button"
          size="sm"
          disabled={!showResult}
          onClick={handleNext}
          className="h-8 rounded-full bg-primary text-white border-0 shadow-sm px-5 text-[11px]"
        >
          {isLastQuestion ? "완료" : "다음"}
        </Button>
      </div>

      {/* Hint Q&A List — Sender: 내가 보낸 질문 */}
      {hints.filter((h) => h.quizIndex === currentIndex + 1 && h.senderId === currentUserId).length > 0 && (
        <div className="flex flex-col gap-2 mt-3 pt-3 border-t border-border/30">
          <p className="text-[10px] font-medium text-muted-foreground">내가 보낸 질문</p>
          {hints
            .filter((h) => h.quizIndex === currentIndex + 1 && h.senderId === currentUserId)
            .map((hint) => (
              <div
                key={hint.id}
                className="bg-secondary/30 rounded-xl px-3 py-2.5 border border-border/20"
              >
                <p className="text-xs font-semibold text-primary">
                  Q. {hint.question}
                </p>
                {hint.answer ? (
                  <p className="text-xs text-foreground mt-1">
                    A. {hint.answer}
                  </p>
                ) : (
                  <p className="text-[11px] text-muted-foreground mt-1">
                    답변 대기 중...
                  </p>
                )}
              </div>
            ))}
        </div>
      )}



      {/* Hint Drawer */}
      <Drawer open={showHintInput} onOpenChange={setShowHintInput}>
        <DrawerContent>
          <div className="mx-auto w-full max-w-md px-4 pb-6">
            <DrawerHeader className="px-0">
              <DrawerTitle>힌트 질문 보내기</DrawerTitle>
            </DrawerHeader>
            <textarea
              value={hintInput}
              onChange={(e) => setHintInput(e.target.value)}
              placeholder="상대방에게 궁금한 점을 물어보세요"
              className="w-full bg-muted/50 text-sm resize-none outline-none placeholder:text-muted-foreground/50 rounded-xl p-3 min-h-[80px] border border-border/30 focus:border-primary/50"
              rows={3}
              autoFocus
            />
            <DrawerFooter className="px-0 pt-4">
              <Button
                onClick={handleSendHint}
                disabled={!hintInput.trim()}
                className="w-full h-11 text-base font-medium"
              >
                <Send className="w-4 h-4 mr-2" />
                보내기
              </Button>
              <Button
                variant="ghost"
                onClick={() => { setShowHintInput(false); setHintInput("") }}
                className="w-full h-10 text-base"
              >
                취소
              </Button>
            </DrawerFooter>
          </div>
        </DrawerContent>
      </Drawer>
    </div>
  )
}
