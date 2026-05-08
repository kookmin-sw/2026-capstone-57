"use client"

import { useState } from "react"
import { MessageCircle, Check, X, Send } from "lucide-react"
import { Button } from "@/components/ui/button"
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
    // Sample: Completed Q&A (answered)
    {
      id: "1",
      from: "me",
      question: "혹시 여름 좋아하시나요?",
      answer: "더운 건 싫어하지만 바다는 좋아해요",
    },
    // Sample: Received question waiting for my answer
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

  const handleExpandAnswer = (noteId: string) => {
    setHintNotes((prev) =>
      prev.map((note) =>
        note.id === noteId ? { ...note, isExpanded: true } : note
      )
    )
  }

  const handleSubmitAnswer = (noteId: string) => {
    if (!answerInput.trim()) return
    setHintNotes((prev) =>
      prev.map((note) =>
        note.id === noteId
          ? { ...note, answer: answerInput.trim(), isExpanded: false }
          : note
      )
    )
    setAnswerInput("")
  }

  const handleDismissAnswer = (noteId: string) => {
    setHintNotes((prev) =>
      prev.map((note) =>
        note.id === noteId ? { ...note, isExpanded: false } : note
      )
    )
  }

  // Separate notes into completed and pending
  const pendingReceivedNotes = hintNotes.filter(
    (n) => n.from === "partner" && !n.answer
  )
  const completedNotes = hintNotes.filter((n) => n.answer)
  const pendingSentNotes = hintNotes.filter(
    (n) => n.from === "me" && !n.answer
  )

  return (
    <div className="bg-card rounded-3xl p-5 shadow-sm border border-border/30">
      {/* Header */}
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-base font-semibold text-foreground">1단계 · 퀴즈</h3>
        <span className="text-xs text-muted-foreground">
          {currentIndex + 1} / {questions.length}
        </span>
      </div>

      {/* Question */}
      <p className="text-base font-semibold text-foreground mb-4">
        {currentQuestion.question}
      </p>

      {/* Options */}
      <div className="space-y-2 mb-4">
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
                "w-full p-3 rounded-2xl text-left text-sm transition-all",
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



      {/* Completed Q&A Notes - Soft Paper Style */}
      {completedNotes.length > 0 && (
        <div className="mb-4 space-y-2">
          {completedNotes.map((note) => (
            <div
              key={note.id}
              className="bg-muted/30 rounded-xl p-3 border border-border/20"
            >
              <p className="text-xs text-muted-foreground mb-1">
                Q. {note.question}
              </p>
              <p className="text-sm text-foreground">
                A. {note.answer}
              </p>
            </div>
          ))}
        </div>
      )}

      {/* Waiting for Partner's Answer */}
      {pendingSentNotes.length > 0 && (
        <div className="mb-4 space-y-2">
          {pendingSentNotes.map((note) => (
            <div
              key={note.id}
              className="bg-primary/5 rounded-xl p-3 border border-primary/10"
            >
              <p className="text-xs text-muted-foreground mb-1">
                Q. {note.question}
              </p>
              <p className="text-xs text-muted-foreground/70 italic">
                답변을 기다리는 중...
              </p>
            </div>
          ))}
        </div>
      )}

      {/* Hint Input - Inline Expand */}
      {showHintInput && (
        <div className="mb-4 bg-secondary/20 rounded-2xl p-4 border border-border/20">
          <p className="text-xs text-muted-foreground mb-2">상대방에게 질문 보내기</p>
          <textarea
            value={hintInput}
            onChange={(e) => setHintInput(e.target.value)}
            placeholder="궁금한 점을 물어보세요"
            className="w-full bg-white/60 text-sm resize-none outline-none placeholder:text-muted-foreground/50 rounded-xl p-3 mb-3 min-h-[60px] border border-border/30"
            rows={2}
          />
          <div className="flex gap-2">
            <Button
              type="button"
              size="sm"
              onClick={handleSendHint}
              disabled={!hintInput.trim()}
              className="flex-1 rounded-full bg-primary hover:bg-primary/90 text-white border-0 text-xs"
            >
              <Send className="w-3 h-3 mr-1" />
              보내기
            </Button>
            <Button
              type="button"
              size="sm"
              variant="ghost"
              onClick={() => {
                setShowHintInput(false)
                setHintInput("")
              }}
              className="rounded-full text-xs text-muted-foreground"
            >
              취소
            </Button>
          </div>
        </div>
      )}

      {/* Actions */}
      <div className="flex items-center justify-between gap-3">
        <Button 
          type="button"
          variant="outline" 
          size="sm"
          onClick={(e) => {
            e.preventDefault()
            setShowHintInput(!showHintInput)
          }}
          className={cn(
            "rounded-full bg-secondary/60 border-0 text-xs",
            showHintInput && "bg-primary/10 text-primary"
          )}
        >
          <MessageCircle className="w-3.5 h-3.5 mr-1" />
          힌트 질문 보내기
        </Button>
        
        <Button
          type="button"
          size="sm"
          disabled={!showResult}
          onClick={handleNext}
          className="rounded-full bg-primary text-white border-0 shadow-sm px-6"
        >
          {isLastQuestion ? "완료" : "다음"}
        </Button>
      </div>
    </div>
  )
}
