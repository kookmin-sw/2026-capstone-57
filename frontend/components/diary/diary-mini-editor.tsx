"use client"

import { useState } from "react"
import { PenLine, Sparkles } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { cn } from "@/lib/utils"
import type { EmotionTag } from "@/types/diary"
import { EMOTION_META } from "@/types/diary"

interface DiaryMiniEditorProps {
  date: Date
  onSave: (content: string, emotion: EmotionTag) => void
  className?: string
}

const emotions: EmotionTag[] = ["HAPPY", "EXCITED", "CALM", "TIRED", "ANXIOUS", "SAD", "ANGRY"]

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const weekdays = ["일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일"]
  const weekday = weekdays[date.getDay()]
  return `${year}년 ${month}월 ${day}일 ${weekday}`
}

export function DiaryMiniEditor({ date, onSave, className }: DiaryMiniEditorProps) {
  const [selectedEmotion, setSelectedEmotion] = useState<EmotionTag | null>(null)
  const [content, setContent] = useState("")

  const isToday = new Date().toDateString() === date.toDateString()

  const handleSave = () => {
    if (!selectedEmotion || !content.trim()) return
    onSave(content, selectedEmotion)
    setContent("")
    setSelectedEmotion(null)
  }

  return (
    <Card className={cn(
      "border-border/50 shadow-sm overflow-hidden",
      "animate-in fade-in slide-in-from-bottom-4 duration-300",
      className
    )}>
      {/* Header */}
      <div className="px-5 py-4 border-b border-border/30 bg-gradient-to-r from-secondary/20 to-secondary/5">
        <div className="flex items-center gap-3">
          <div className="size-10 rounded-xl bg-secondary/50 flex items-center justify-center">
            <PenLine className="size-5 text-foreground/70" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="font-semibold text-foreground">
                {isToday ? "오늘의 일기" : formatDate(date)}
              </h3>
              {isToday && (
                <span className="flex items-center gap-1 text-xs text-accent">
                  <Sparkles className="size-3" />
                  작성하기
                </span>
              )}
            </div>
            <p className="text-xs text-muted-foreground mt-0.5">
              오늘 하루는 어땠나요?
            </p>
          </div>
        </div>
      </div>

      <CardContent className="p-4 space-y-4">
        {/* Emotion Selector */}
        <div className="space-y-2">
          <p className="text-sm font-medium text-foreground">기분을 선택해주세요</p>
          <div className="flex flex-wrap gap-2">
            {emotions.map((emotion) => {
              const meta = EMOTION_META[emotion]
              const isSelected = selectedEmotion === emotion
              
              return (
                <button
                  key={emotion}
                  onClick={() => setSelectedEmotion(emotion)}
                  className={cn(
                    "flex items-center gap-1.5 px-3 py-1.5 rounded-full border transition-all duration-200",
                    "text-xs font-medium",
                    isSelected
                      ? `${meta.bgColor} ${meta.color} border-current shadow-sm scale-105`
                      : "bg-card border-border text-muted-foreground hover:border-primary/30 active:scale-95"
                  )}
                >
                  <span>{meta.weatherIcon}</span>
                  <span>{meta.label}</span>
                </button>
              )
            })}
          </div>
        </div>

        {/* Content Input */}
        <div className="space-y-2">
          <Textarea
            placeholder="오늘 있었던 일, 느낀 감정을 자유롭게 적어보세요..."
            value={content}
            onChange={(e) => setContent(e.target.value)}
            className={cn(
              "min-h-[120px] resize-none",
              "bg-muted/30 border-border/50",
              "focus:border-primary/50 focus:ring-primary/20",
              "placeholder:text-muted-foreground/50"
            )}
          />
        </div>

        {/* Save Button */}
        <Button
          onClick={handleSave}
          disabled={!selectedEmotion || !content.trim()}
          className="w-full bg-primary hover:bg-primary/90"
        >
          일기 저장하기
        </Button>
      </CardContent>
    </Card>
  )
}
