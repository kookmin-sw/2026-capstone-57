"use client"

import { useState } from "react"
import { X, Send } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

interface HintAnswerSheetProps {
  question: string
  partnerName?: string
  isOpen: boolean
  onClose: () => void
  onSubmit: (answer: string) => void
}

export function HintAnswerSheet({
  question,
  partnerName = "상대방",
  isOpen,
  onClose,
  onSubmit,
}: HintAnswerSheetProps) {
  const [answer, setAnswer] = useState("")

  const handleSubmit = () => {
    if (!answer.trim()) return
    onSubmit(answer.trim())
    setAnswer("")
  }

  if (!isOpen) return null

  return (
    <>
      {/* Backdrop */}
      <div 
        className="fixed inset-0 bg-black/40 z-50 transition-opacity"
        onClick={onClose}
      />
      
      {/* Sheet */}
      <div className={cn(
        "fixed bottom-0 left-0 right-0 z-50",
        "bg-background rounded-t-3xl shadow-xl",
        "animate-in slide-in-from-bottom duration-300",
        "max-w-[430px] mx-auto"
      )}>
        {/* Handle */}
        <div className="flex justify-center pt-3 pb-2">
          <div className="w-10 h-1 rounded-full bg-muted-foreground/20" />
        </div>

        {/* Header */}
        <div className="flex items-center justify-between px-5 pb-3">
          <h3 className="text-base font-semibold text-foreground">힌트 답변</h3>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-full hover:bg-muted transition-colors"
          >
            <X className="w-5 h-5 text-muted-foreground" />
          </button>
        </div>

        {/* Content */}
        <div className="px-5 pb-6">
          {/* Question Display */}
          <div className="bg-secondary/30 rounded-2xl p-4 mb-4">
            <p className="text-xs text-muted-foreground mb-1.5">
              {partnerName}님의 질문
            </p>
            <p className="text-sm text-foreground leading-relaxed">
              &quot;{question}&quot;
            </p>
          </div>

          {/* Answer Input */}
          <textarea
            value={answer}
            onChange={(e) => setAnswer(e.target.value)}
            placeholder="부담 없이 답변해보세요"
            className={cn(
              "w-full bg-muted/30 text-sm resize-none outline-none",
              "placeholder:text-muted-foreground/50",
              "rounded-2xl p-4 min-h-[100px]",
              "border border-border/30 focus:border-primary/50 transition-colors"
            )}
            rows={3}
          />

          {/* Submit Button */}
          <Button
            type="button"
            onClick={handleSubmit}
            disabled={!answer.trim()}
            className="w-full mt-4 rounded-full bg-primary hover:bg-primary/90 text-white h-12"
          >
            <Send className="w-4 h-4 mr-2" />
            답변 보내기
          </Button>
        </div>

        {/* Safe Area */}
        <div className="pb-safe" />
      </div>
    </>
  )
}
