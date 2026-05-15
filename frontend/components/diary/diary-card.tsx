"use client"

import { cn } from "@/lib/utils"
import { Card, CardContent } from "@/components/ui/card"
import type { DiaryEntry } from "@/types/diary"
import { EMOTION_META, getStreakLevel } from "@/types/diary"

interface DiaryCardProps {
  entry: DiaryEntry
  onClick?: () => void
  className?: string
}

function formatDate(dateStr: string) {
  const date = new Date(dateStr)
  const month = date.getMonth() + 1
  const day = date.getDate()
  const weekday = ["일", "월", "화", "수", "목", "금", "토"][date.getDay()]
  return `${month}월 ${day}일 ${weekday}요일`
}

export function DiaryCard({ entry, onClick, className }: DiaryCardProps) {
  const emotionMeta = EMOTION_META[entry.emotionTag]
  const streakLevel = getStreakLevel(entry.streakCount)

  return (
    <Card 
      className={cn(
        "cursor-pointer transition-all hover:shadow-md",
        "border-l-4",
        className
      )}
      style={{ borderLeftColor: `var(--${entry.emotionTag.toLowerCase()}-color, #e5e7eb)` }}
      onClick={onClick}
    >
      <CardContent className="p-4">
        <div className="flex items-start justify-between gap-3">
          {/* Content */}
          <div className="flex-1 min-w-0 space-y-2">
            {/* Date & Emotion */}
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium text-foreground">
                {formatDate(entry.entryDate)}
              </span>
              <span className={cn(
                "inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs",
                emotionMeta.bgColor,
                emotionMeta.color
              )}>
                <span>{emotionMeta.weatherIcon}</span>
                {emotionMeta.label}
              </span>
            </div>

            {/* Preview */}
            <p className="text-sm text-muted-foreground line-clamp-2">
              {entry.content}
            </p>
          </div>

          {/* Streak badge */}
          {entry.streakCount > 0 && (
            <div className={cn(
              "shrink-0 flex items-center gap-1 px-2 py-1 rounded-full",
              "bg-amber-50 text-amber-600 text-xs font-medium"
            )}>
              {streakLevel ? (
                <span>{streakLevel.emoji}</span>
              ) : (
                <span>🔥</span>
              )}
              {entry.streakCount}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
