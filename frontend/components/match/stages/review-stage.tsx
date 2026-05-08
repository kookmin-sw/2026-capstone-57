"use client"

import { useState } from "react"
import { Star } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

interface ReviewStageProps {
  onSave: (content: string, rating: number, mode: "ai" | "free") => void
}

const aiQuestions = [
  "오늘 만남에서 가장 기억에 남는 순간은 언제였나요?",
  "상대방의 어떤 점이 인상 깊었나요?",
  "다음에 만나면 함께 해보고 싶은 것이 있나요?",
]

export function ReviewStage({ onSave }: ReviewStageProps) {
  const [mode, setMode] = useState<"ai" | "free">("ai")
  const [content, setContent] = useState("")
  const [rating, setRating] = useState(0)
  const [aiQuestionIndex, setAiQuestionIndex] = useState(0)
  const [aiAnswers, setAiAnswers] = useState<string[]>([])

  const handleAiNext = () => {
    if (content.trim()) {
      setAiAnswers((prev) => [...prev, content])
      setContent("")
      
      if (aiQuestionIndex < aiQuestions.length - 1) {
        setAiQuestionIndex((prev) => prev + 1)
      } else {
        // All questions answered, compile answers
        const compiledContent = [...aiAnswers, content].join("\n\n")
        setContent(compiledContent)
        setMode("free") // Switch to free mode to show rating
      }
    }
  }

  const handleSave = () => {
    if (content.trim() && rating > 0) {
      onSave(content, rating, mode)
    }
  }

  return (
    <div className="bg-card rounded-3xl p-5 shadow-sm border border-border/30">
      {/* Header */}
      <h3 className="text-base font-semibold text-foreground mb-4">5단계 · 회고</h3>

      {/* Mode toggle */}
      <div className="bg-muted/40 rounded-2xl p-1 flex mb-4">
        <button
          onClick={() => setMode("ai")}
          className={cn(
            "flex-1 py-2 rounded-xl text-sm font-medium transition-all",
            mode === "ai" 
              ? "gradient-gem text-white shadow-gem" 
              : "text-muted-foreground"
          )}
        >
          AI와 함께
        </button>
        <button
          onClick={() => setMode("free")}
          className={cn(
            "flex-1 py-2 rounded-xl text-sm font-medium transition-all",
            mode === "free" 
              ? "gradient-gem text-white shadow-gem" 
              : "text-muted-foreground"
          )}
        >
          직접 작성
        </button>
      </div>

      {/* Content */}
      {mode === "ai" ? (
        <div className="space-y-4">
          {/* AI Question */}
          <div className="bg-secondary/50 rounded-2xl p-4">
            <p className="text-sm text-foreground">
              {aiQuestions[aiQuestionIndex]}
            </p>
          </div>

          {/* Answer input */}
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="자유롭게 답해보세요..."
            rows={4}
            className="w-full p-4 rounded-2xl border border-border/50 bg-background text-sm resize-none outline-none focus:ring-2 focus:ring-primary/30"
          />

          <Button
            onClick={handleAiNext}
            disabled={!content.trim()}
            className="w-full rounded-full gradient-gem text-white border-0 shadow-gem"
          >
            {aiQuestionIndex < aiQuestions.length - 1 ? "다음 질문" : "회고 완성하기"}
          </Button>
        </div>
      ) : (
        <div className="space-y-4">
          {/* Free writing */}
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="오늘의 만남은 어땠나요?"
            rows={6}
            className="w-full p-4 rounded-2xl border border-border/50 bg-background text-sm resize-none outline-none focus:ring-2 focus:ring-primary/30"
          />

          {/* Rating */}
          <div className="flex justify-center gap-2">
            {[1, 2, 3, 4, 5].map((star) => (
              <button
                key={star}
                onClick={() => setRating(star)}
                className={cn(
                  "w-10 h-10 rounded-xl flex items-center justify-center transition-all",
                  star <= rating 
                    ? "bg-secondary text-accent" 
                    : "bg-muted/40 text-muted-foreground hover:bg-muted/60"
                )}
              >
                <Star className={cn(
                  "w-5 h-5",
                  star <= rating && "fill-current"
                )} />
              </button>
            ))}
          </div>

          <Button
            onClick={handleSave}
            disabled={!content.trim() || rating === 0}
            className="w-full rounded-full gradient-gem text-white border-0 shadow-gem"
          >
            회고 저장 (+50 XP)
          </Button>
        </div>
      )}
    </div>
  )
}
