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
        const compiledContent = [...aiAnswers, content].join("\n\n")
        setContent(compiledContent)
        setMode("free")
      }
    }
  }

  const handleSave = () => {
    if (content.trim() && rating > 0) {
      onSave(content, rating, mode)
    }
  }

  return (
    <div className="h-full bg-card rounded-2xl p-4 shadow-sm border border-border/30 flex flex-col">
      {/* Header */}
      <h3 className="text-sm font-semibold text-foreground mb-2 shrink-0">5단계 · 회고</h3>

      {/* Mode toggle */}
      <div className="bg-muted/40 rounded-xl p-0.5 flex mb-3 shrink-0">
        <button
          onClick={() => setMode("ai")}
          className={cn(
            "flex-1 py-1.5 rounded-lg text-xs font-medium transition-all",
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
            "flex-1 py-1.5 rounded-lg text-xs font-medium transition-all",
            mode === "free"
              ? "gradient-gem text-white shadow-gem"
              : "text-muted-foreground"
          )}
        >
          직접 작성
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 flex flex-col min-h-0">
        {mode === "ai" ? (
          <>
            <div className="bg-secondary/50 rounded-xl p-3 mb-2 shrink-0">
              <p className="text-xs text-foreground">{aiQuestions[aiQuestionIndex]}</p>
            </div>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="자유롭게 답해보세요..."
              className="flex-1 w-full p-3 rounded-xl border border-border/50 bg-background text-xs resize-none outline-none focus:ring-1 focus:ring-primary/30 min-h-0"
            />
            <Button
              onClick={handleAiNext}
              disabled={!content.trim()}
              size="sm"
              className="w-full mt-2 rounded-full gradient-gem text-white border-0 shadow-gem text-xs h-9 shrink-0"
            >
              {aiQuestionIndex < aiQuestions.length - 1 ? "다음 질문" : "회고 완성하기"}
            </Button>
          </>
        ) : (
          <>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="오늘의 만남은 어땠나요?"
              className="flex-1 w-full p-3 rounded-xl border border-border/50 bg-background text-xs resize-none outline-none focus:ring-1 focus:ring-primary/30 min-h-0"
            />
            {/* Rating */}
            <div className="flex justify-center gap-1.5 my-2 shrink-0">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  onClick={() => setRating(star)}
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
            <Button
              onClick={handleSave}
              disabled={!content.trim() || rating === 0}
              size="sm"
              className="w-full rounded-full gradient-gem text-white border-0 shadow-gem text-xs h-9 shrink-0"
            >
              회고 저장 (+50 XP)
            </Button>
          </>
        )}
      </div>
    </div>
  )
}
