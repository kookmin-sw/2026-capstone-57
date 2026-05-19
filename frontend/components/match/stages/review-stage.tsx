"use client"

import { useState, useEffect, useRef } from "react"
import { Star, Sparkles, CheckCircle2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Switch } from "@/components/ui/switch"
import { cn } from "@/lib/utils"
import { useReviewStage } from "@/hooks/match/use-review-stage"
import type { UseReviewStageReturn } from "@/hooks/match/use-review-stage"

interface ReviewStageProps {
  matchId: string
  onComplete?: () => void
}

export function ReviewStage({ matchId, onComplete }: ReviewStageProps) {
  const review = useReviewStage({ matchId, onComplete })

  useEffect(() => {
    review.loadReviewData()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="h-full bg-card rounded-2xl p-4 shadow-sm border border-border/30 flex flex-col">
      <h3 className="text-sm font-semibold text-foreground mb-2 shrink-0">5단계 · 회고</h3>

      {review.phase === "loading" && <LoadingView />}
      {review.phase === "completed" && <CompletedView review={review} />}
      {review.phase === "mode-select" && <ModeSelectView review={review} />}
      {review.phase === "ai-conversation" && <AIConversationView review={review} />}
      {review.phase === "ai-generating" && <GeneratingView />}
      {review.phase === "ai-confirm" && <AIConfirmView review={review} />}
      {review.phase === "direct-write" && <DirectWriteView review={review} />}
    </div>
  )
}

// ─── Loading ─────────────────────────────────────────────────────────────────

function LoadingView() {
  return (
    <div className="flex-1 flex flex-col items-center justify-center gap-3">
      <div className="flex items-center gap-1">
        <span className="w-2 h-2 rounded-full bg-primary animate-bounce [animation-delay:0ms]" />
        <span className="w-2 h-2 rounded-full bg-primary animate-bounce [animation-delay:150ms]" />
        <span className="w-2 h-2 rounded-full bg-primary animate-bounce [animation-delay:300ms]" />
      </div>
      <p className="text-xs text-muted-foreground">회고 정보를 불러오는 중...</p>
    </div>
  )
}

// ─── Completed (readonly) ────────────────────────────────────────────────────

function CompletedView({ review }: { review: UseReviewStageReturn }) {
  const data = review.completedReview
  if (!data) return null

  return (
    <div className="flex-1 flex flex-col gap-3 overflow-y-auto">
      <div className="flex items-center gap-2 bg-green-50 dark:bg-green-950/30 rounded-xl p-3">
        <CheckCircle2 className="w-4 h-4 text-green-600 shrink-0" />
        <p className="text-xs text-green-700 dark:text-green-400 font-medium">
          이미 회고를 작성했어요
        </p>
      </div>

      <div className="bg-secondary/50 rounded-xl p-3">
        <p className="text-[10px] text-muted-foreground mb-1">
          {data.mode === "AI_ASSISTED" ? "AI와 함께 작성" : "직접 작성"}
        </p>
        <p className="text-xs text-foreground whitespace-pre-wrap">{data.reflection}</p>
      </div>

      <div className="flex items-center justify-between">
        <div className="flex gap-0.5">
          {[1, 2, 3, 4, 5].map((star) => (
            <Star
              key={star}
              className={cn(
                "w-4 h-4",
                star <= data.satisfaction
                  ? "text-accent fill-current"
                  : "text-muted-foreground"
              )}
            />
          ))}
        </div>
        <span className="text-[10px] text-muted-foreground">
          {data.wantToMeetAgain ? "다시 만나고 싶어요 💛" : "아쉽지만 다음에요"}
        </span>
      </div>
    </div>
  )
}

// ─── Mode Select ─────────────────────────────────────────────────────────────

function ModeSelectView({ review }: { review: UseReviewStageReturn }) {
  return (
    <div className="flex-1 flex flex-col items-center justify-center gap-4">
      <p className="text-xs text-muted-foreground text-center">
        오늘의 만남을 돌아볼 방법을 선택해주세요
      </p>

      <div className="w-full flex flex-col gap-2">
        <button
          onClick={() => review.handleModeSelect("ai")}
          disabled={review.isSubmitting}
          className="w-full p-4 rounded-xl border border-border/50 bg-background hover:bg-secondary/50 transition-all text-left disabled:opacity-50"
        >
          <div className="flex items-center gap-2 mb-1">
            <Sparkles className="w-4 h-4 text-primary" />
            <span className="text-sm font-medium text-foreground">AI와 함께 작성</span>
          </div>
          <p className="text-[10px] text-muted-foreground">
            AI가 질문을 던져주고, 답변을 바탕으로 회고를 정리해줘요
          </p>
        </button>

        <button
          onClick={() => review.handleModeSelect("free")}
          disabled={review.isSubmitting}
          className="w-full p-4 rounded-xl border border-border/50 bg-background hover:bg-secondary/50 transition-all text-left disabled:opacity-50"
        >
          <div className="flex items-center gap-2 mb-1">
            <span className="text-sm">✍️</span>
            <span className="text-sm font-medium text-foreground">직접 작성</span>
          </div>
          <p className="text-[10px] text-muted-foreground">
            자유롭게 오늘의 만남을 기록해요
          </p>
        </button>
      </div>

      {review.isSubmitting && (
        <p className="text-[10px] text-muted-foreground animate-pulse">준비 중...</p>
      )}
    </div>
  )
}

// ─── AI Conversation (멀티턴) ────────────────────────────────────────────────

function AIConversationView({ review }: { review: UseReviewStageReturn }) {
  const [answer, setAnswer] = useState("")
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const scrollRef = useRef<HTMLDivElement>(null)

  // textarea 자동 높이 조절
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto"
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 160)}px`
    }
  }, [answer])

  // 새 질문이 오면 스크롤 하단으로
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [review.conversationHistory, review.currentQuestion])

  const isOverLimit = answer.length > 5000

  const handleSubmit = async () => {
    if (!answer.trim() || isOverLimit || review.isSubmitting) return
    const submitted = answer
    setAnswer("")
    await review.handleSubmitAnswer(submitted)
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      handleSubmit()
    }
  }

  return (
    <div className="flex-1 flex flex-col min-h-0">
      {/* Progress */}
      <div className="flex items-center justify-between mb-2 shrink-0">
        <span className="text-[10px] text-muted-foreground">
          질문 {review.currentTurn} / {review.maxTurns}
        </span>
        <div className="flex gap-0.5">
          {Array.from({ length: review.maxTurns }).map((_, i) => (
            <div
              key={i}
              className={cn(
                "w-2 h-2 rounded-full transition-colors",
                i < review.currentTurn - 1
                  ? "bg-primary"
                  : i === review.currentTurn - 1
                  ? "bg-primary/60"
                  : "bg-muted"
              )}
            />
          ))}
        </div>
      </div>

      {/* Conversation history + current question */}
      <div ref={scrollRef} className="flex-1 overflow-y-auto space-y-2 mb-2 min-h-0">
        {review.conversationHistory.map((turn) => (
          <div key={turn.turnNumber} className="space-y-1.5">
            {/* AI question */}
            <div className="bg-secondary/50 rounded-xl p-2.5">
              <p className="text-[10px] text-muted-foreground mb-0.5">Q{turn.turnNumber}</p>
              <p className="text-xs text-foreground">{turn.question}</p>
            </div>
            {/* User answer */}
            <div className="bg-primary/5 rounded-xl p-2.5 ml-4">
              <p className="text-xs text-foreground">{turn.answer}</p>
            </div>
          </div>
        ))}

        {/* Current question */}
        {review.currentQuestion && (
          <div className="bg-secondary/50 rounded-xl p-2.5">
            <p className="text-[10px] text-muted-foreground mb-0.5">Q{review.currentTurn}</p>
            <p className="text-xs text-foreground">{review.currentQuestion}</p>
          </div>
        )}
      </div>

      {/* Answer input */}
      <div className="shrink-0">
        <textarea
          ref={textareaRef}
          value={answer}
          onChange={(e) => setAnswer(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="자유롭게 답해보세요..."
          maxLength={5000}
          className="w-full p-3 rounded-xl border border-border/50 bg-background text-xs resize-none outline-none focus:ring-1 focus:ring-primary/30 min-h-[60px] max-h-[160px]"
        />

        <div className="flex items-center justify-between mt-1">
          <span className={cn("text-[10px]", isOverLimit ? "text-red-500" : "text-muted-foreground")}>
            {answer.length > 0 && `${answer.length} / 5,000`}
          </span>
          <span className="text-[10px] text-muted-foreground">Shift+Enter로 줄바꿈</span>
        </div>

        <Button
          onClick={handleSubmit}
          disabled={!answer.trim() || isOverLimit || review.isSubmitting}
          size="sm"
          className="w-full mt-2 rounded-full gradient-gem text-white border-0 shadow-gem text-xs h-9"
        >
          {review.isSubmitting
            ? "제출 중..."
            : review.currentTurn >= review.maxTurns
            ? "회고 생성하기"
            : "다음 질문"}
        </Button>

        {review.error && (
          <p className="text-[10px] text-red-500 mt-1 text-center">{review.error}</p>
        )}
      </div>
    </div>
  )
}

// ─── Generating (loading) ────────────────────────────────────────────────────

function GeneratingView() {
  return (
    <div className="flex-1 flex flex-col items-center justify-center gap-4">
      <div className="relative">
        <Sparkles className="w-8 h-8 text-primary animate-pulse" />
      </div>
      <div className="text-center">
        <p className="text-sm font-medium text-foreground mb-1">AI가 회고를 정리하고 있어요</p>
        <p className="text-[10px] text-muted-foreground">답변을 바탕으로 회고를 작성 중...</p>
      </div>
      {/* Skeleton */}
      <div className="w-full space-y-2 mt-2">
        <div className="h-3 bg-muted/60 rounded-full animate-pulse w-full" />
        <div className="h-3 bg-muted/60 rounded-full animate-pulse w-4/5" />
        <div className="h-3 bg-muted/60 rounded-full animate-pulse w-3/5" />
        <div className="h-3 bg-muted/60 rounded-full animate-pulse w-4/5" />
      </div>
    </div>
  )
}

// ─── AI Confirm ──────────────────────────────────────────────────────────────

function AIConfirmView({ review }: { review: UseReviewStageReturn }) {
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto"
      textareaRef.current.style.height = `${textareaRef.current.scrollHeight}px`
    }
  }, [review.reflection])

  const isOverLimit = review.reflection.length > 5000

  return (
    <div className="flex-1 flex flex-col min-h-0">
      {/* AI badge */}
      <div className="flex items-center gap-1.5 mb-2 shrink-0">
        <Sparkles className="w-3.5 h-3.5 text-primary" />
        <span className="text-[10px] font-medium text-primary">AI가 회고를 정리했어요</span>
        <span className="text-[10px] text-muted-foreground ml-auto">수정 가능</span>
      </div>

      {/* Editable content */}
      <textarea
        ref={textareaRef}
        value={review.reflection}
        onChange={(e) => review.setReflection(e.target.value)}
        maxLength={5000}
        className="flex-1 w-full p-3 rounded-xl border border-border/50 bg-background text-xs resize-none outline-none focus:ring-1 focus:ring-primary/30 min-h-[100px]"
      />

      <div className="flex justify-end mt-1 shrink-0">
        <span className={cn("text-[10px]", isOverLimit ? "text-red-500" : "text-muted-foreground")}>
          {review.reflection.length} / 5,000
        </span>
      </div>

      {/* Rating */}
      <RatingSelector rating={review.rating} onRate={review.setRating} />

      {/* Want to meet again */}
      <MeetAgainToggle value={review.wantToMeetAgain} onChange={review.setWantToMeetAgain} />

      <Button
        onClick={review.handleConfirmReview}
        disabled={!review.reflection.trim() || review.rating === 0 || isOverLimit || review.isSubmitting}
        size="sm"
        className="w-full mt-2 rounded-full gradient-gem text-white border-0 shadow-gem text-xs h-9 shrink-0"
      >
        {review.isSubmitting ? "저장 중..." : "회고 저장 (+50 XP)"}
      </Button>

      {review.error && (
        <p className="text-[10px] text-red-500 mt-1 text-center">{review.error}</p>
      )}
    </div>
  )
}

// ─── Direct Write ────────────────────────────────────────────────────────────

function DirectWriteView({ review }: { review: UseReviewStageReturn }) {
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto"
      textareaRef.current.style.height = `${textareaRef.current.scrollHeight}px`
    }
  }, [review.reflection])

  const isOverLimit = review.reflection.length > 5000

  return (
    <div className="flex-1 flex flex-col min-h-0">
      <textarea
        ref={textareaRef}
        value={review.reflection}
        onChange={(e) => review.setReflection(e.target.value)}
        placeholder="오늘의 만남은 어땠나요? 자유롭게 기록해보세요."
        maxLength={5000}
        className="flex-1 w-full p-3 rounded-xl border border-border/50 bg-background text-xs resize-none outline-none focus:ring-1 focus:ring-primary/30 min-h-[120px]"
      />

      <div className="flex justify-end mt-1 shrink-0">
        <span className={cn("text-[10px]", isOverLimit ? "text-red-500" : "text-muted-foreground")}>
          {review.reflection.length} / 5,000
        </span>
      </div>

      {/* Rating */}
      <RatingSelector rating={review.rating} onRate={review.setRating} />

      {/* Want to meet again */}
      <MeetAgainToggle value={review.wantToMeetAgain} onChange={review.setWantToMeetAgain} />

      <Button
        onClick={review.handleDirectSubmit}
        disabled={!review.reflection.trim() || review.rating === 0 || isOverLimit || review.isSubmitting}
        size="sm"
        className="w-full mt-2 rounded-full gradient-gem text-white border-0 shadow-gem text-xs h-9 shrink-0"
      >
        {review.isSubmitting ? "저장 중..." : "회고 저장 (+50 XP)"}
      </Button>

      {review.error && (
        <p className="text-[10px] text-red-500 mt-1 text-center">{review.error}</p>
      )}
    </div>
  )
}

// ─── Shared Components ───────────────────────────────────────────────────────

function RatingSelector({ rating, onRate }: { rating: number; onRate: (v: number) => void }) {
  return (
    <div className="flex items-center justify-center gap-1.5 my-2 shrink-0">
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          onClick={() => onRate(star)}
          className={cn(
            "w-8 h-8 rounded-lg flex items-center justify-center transition-all",
            star <= rating
              ? "bg-secondary text-accent"
              : "bg-muted/40 text-muted-foreground hover:bg-muted/60"
          )}
        >
          <Star className={cn("w-4 h-4", star <= rating && "fill-current")} />
        </button>
      ))}
    </div>
  )
}

function MeetAgainToggle({
  value,
  onChange,
}: {
  value: boolean
  onChange: (v: boolean) => void
}) {
  return (
    <div className="flex items-center justify-between bg-secondary/30 rounded-xl px-3 py-2 my-1 shrink-0">
      <span className="text-xs text-foreground">다시 만나고 싶나요?</span>
      <Switch checked={value} onCheckedChange={onChange} />
    </div>
  )
}
