"use client"

import { Card, CardContent } from "@/components/ui/card"
import { cn } from "@/lib/utils"
import type { DiaryEntry } from "@/types/diary"
import { EMOTION_META } from "@/types/diary"

interface DiaryDetailCardProps {
  entry: DiaryEntry
  className?: string
}

function formatDate(dateStr: string) {
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const weekdays = ["일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일"]
  const weekday = weekdays[date.getDay()]
  return `${year}년 ${month}월 ${day}일 ${weekday}`
}

function formatTime(isoString: string) {
  const date = new Date(isoString)
  const hours = date.getHours()
  const minutes = String(date.getMinutes()).padStart(2, "0")
  const period = hours < 12 ? "오전" : "오후"
  const displayHours = hours % 12 || 12
  return `${period} ${displayHours}:${minutes}`
}

export function DiaryDetailCard({ entry, className }: DiaryDetailCardProps) {
  const meta = EMOTION_META[entry.emotionTag]

  return (
    <Card className={cn(
      "border-border/50 shadow-sm overflow-hidden",
      "animate-in fade-in slide-in-from-bottom-4 duration-300",
      className
    )}>
      {/* Header with emotion */}
      <div className={cn(
        "px-5 py-4 border-b border-border/30",
        meta.bgColor
      )}>
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm text-muted-foreground mb-1">
              {formatDate(entry.entryDate)}
            </p>
            <div className="flex items-center gap-2">
              <span className="text-2xl">{meta.weatherIcon}</span>
              <span className={cn("font-semibold", meta.color)}>
                {meta.label}
              </span>
            </div>
          </div>
          <div className="text-right">
            <p className="text-xs text-muted-foreground">
              {formatTime(entry.createdAt)}에 작성
            </p>
          </div>
        </div>
      </div>

      {/* Content */}
      <CardContent className="p-5">
        <div className="relative">
          {/* Paper texture lines effect */}
          <div 
            className="absolute inset-0 pointer-events-none opacity-30"
            style={{
              backgroundImage: "repeating-linear-gradient(transparent, transparent 27px, #e5e5e5 28px)",
            }}
          />
          
          <p className="text-foreground leading-7 whitespace-pre-wrap relative z-10 font-light">
            {entry.content}
          </p>
        </div>
      </CardContent>
    </Card>
  )
}
