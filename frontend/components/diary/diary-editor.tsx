"use client"

import { useState } from "react"
import { Sparkles } from "lucide-react"
import { cn } from "@/lib/utils"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { EmotionSelector } from "./emotion-selector"
import { StreakBadge } from "./streak-badge"
import type { EmotionTag } from "@/types/diary"

interface DiaryEditorProps {
  date: Date
  initialContent?: string
  initialEmotion?: EmotionTag
  currentStreak: number
  longestStreak: number
  onSave: (content: string, emotion: EmotionTag) => void
  className?: string
}

function formatDate(date: Date) {
  const month = date.getMonth() + 1
  const day = date.getDate()
  const weekday = ["일", "월", "화", "수", "목", "금", "토"][date.getDay()]
  return `${month}월 ${day}일 ${weekday}요일`
}

export function DiaryEditor({
  date,
  initialContent = "",
  initialEmotion,
  currentStreak,
  longestStreak,
  onSave,
  className,
}: DiaryEditorProps) {
  const [content, setContent] = useState(initialContent)
  const [emotion, setEmotion] = useState<EmotionTag | null>(initialEmotion || null)

  const canSave = content.trim().length > 0 && emotion !== null

  const handleSave = () => {
    if (canSave && emotion) {
      onSave(content, emotion)
    }
  }

  return (
    <div className={cn("space-y-4", className)}>
      {/* Date Header */}
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-foreground">
          {formatDate(date)}
        </h2>
        <StreakBadge currentStreak={currentStreak} />
      </div>

      {/* Emotion Selector */}
      <Card>
        <CardContent className="p-4">
          <EmotionSelector value={emotion} onChange={setEmotion} />
        </CardContent>
      </Card>

      {/* Content Editor */}
      <Card className="overflow-hidden">
        <CardContent className="p-0">
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="오늘 하루는 어땠나요?"
            className={cn(
              "w-full min-h-[240px] p-4 resize-none",
              "bg-transparent text-foreground placeholder:text-muted-foreground",
              "focus:outline-none",
              "text-sm leading-relaxed"
            )}
          />
          
          {/* Character count */}
          <div className="px-4 pb-3 flex items-center justify-between text-xs text-muted-foreground">
            <span>{content.length}자</span>
            {content.length > 0 && (
              <span className="flex items-center gap-1 text-primary">
                <Sparkles className="w-3 h-3" />
                작성 완료 시 +10 경험치
              </span>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Streak Info */}
      <Card className="bg-gradient-to-r from-amber-50/50 to-orange-50/50">
        <CardContent className="p-4">
          <StreakBadge 
            currentStreak={currentStreak} 
            longestStreak={longestStreak}
            showDetails 
          />
        </CardContent>
      </Card>

      {/* Save Button */}
      <Button
        onClick={handleSave}
        disabled={!canSave}
        className="w-full"
        size="lg"
      >
        일기 저장하기
      </Button>
    </div>
  )
}
