"use client"

import { useState } from "react"
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
import type { QuizQuestion } from "@/types/match"

interface HintNote {
  id: string
  from: "me" | "partner"
  question: string
  answer?: string
  isExpanded?: boolean
}

interface QuizStageProps {
  questions: QuizQuestion[]
  onComplete: () => void
}

export function QuizStage({ questions, onComplete }: QuizStageProps) {
  const [currentIndex, setCurrentIndex] = useState(0)
  const [selectedOption, setSelectedOption] = useState<number | null>(null)
  const [showResult, setShowResult] = useState(false)
  const [showHintInput, setShowHintInput] = useState(false)
  const [hintInput, setHintInput] = useState("")
  const [answerInput, setAnswerInput] = useState("")
  const [hintNotes, setHintNotes] = useState<HintNote[]>([
    {
      id: "1",
      from: "me",
      question: "혹시 여름 좋아하시나요?",
      answer: "더운 건 싫어하지만 바다는 좋아해요",
    },
    {
      id: "2",
      from: "partner",
      question: "요즘 가장 자주 가는 장소는 어디인가요?",
      isExpanded: false,
    },
  ])

  const currentQuestion = questions[currentIndex]
  const isLastQuestion = currentIndex === questions.length - 1

  const handleSelect = (optionIndex: number) => {
    if (showResult) return
    setSelectedOption(optionIndex)
    setShowResult(true)
  }

  const handleNext = () => {
    if (isLastQuestion) {
      onComplete()
    } else {
      setCurrentIndex((prev) => prev + 1)
      setSelectedOption(null)
      setShowResult(false)
    }
  }

  const handleSendHint = () => {
    if (!hintInput.trim()) return
    const newNote: HintNote = {
      id: Date.now().toString(),
      from: "me",
      question: hintInput.trim(),
    }
    setHintNotes((prev) => [...prev, newNote])
    setHintInput("")
    setShowHintInput(false)
  }

  const completedNotes = hintNotes.filter((n) => n.answer)
  const pendingSentNotes = hintNotes.filter((n) => n.from === "me" && !n.answer)

  return (
    <div className="h-full flex flex-col bg-card rounded-2xl p-4 shadow-sm border border-border/30">
      {/* Header */}
      <div className="flex justify-between items-center mb-2 shrink-0">
        <h3 className="text-sm font-semibold text-foreground">1단계 · 퀴즈</h3>
        <span className="text-[11px] text-muted-foreground">
          {currentIndex + 1} / {questions.length}
        </span>
      </div>

      {/* Question */}
      <p className="text-sm font-semibold text-foreground mb-3 shrink-0">
        {currentQuestion.question}
      </p>

      {/* Options - takes available space */}
      <div className="flex-1 flex flex-col justify-center gap-2 min-h-0 overflow-hidden">
        {currentQuestion.options.map((option, index) => {
          const isSelected = selectedOption === index
          const isCorrectOption = index === currentQuestion.correctIndex

          return (
            <button
              key={index}
              type="button"
              onClick={() => handleSelect(index)}
              disabled={showResult}
              className={cn(
                "w-full py-2 px-3 rounded-xl text-left text-sm transition-all shrink-0",
                "border bg-secondary/40",
                !showResult && "hover:bg-secondary/60 active:scale-[0.98]",
                showResult && isCorrectOption && "border-accent bg-accent/15 text-accent",
                showResult && isSelected && !isCorrectOption && "border-destructive/60 bg-destructive/10 text-destructive"
              )}
            >
              <div className="flex items-center justify-between">
                <span>{option}</span>
                {showResult && isCorrectOption && <Check className="w-4 h-4 text-accent" />}
                {showResult && isSelected && !isCorrectOption && <X className="w-4 h-4 text-destructive" />}
              </div>
            </button>
          )
        })}
      </div>

      {/* Actions */}
      <div className="flex items-center justify-between gap-2 mt-3 shrink-0">
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
