"use client"

import { cn } from "@/lib/utils"
import type { EmotionTag } from "@/types/diary"
import { EMOTION_META } from "@/types/diary"

interface EmotionSelectorProps {
  value: EmotionTag | null
  onChange: (emotion: EmotionTag) => void
  className?: string
}

const emotions: EmotionTag[] = [
  "HAPPY",
  "EXCITED",
  "CALM",
  "TIRED",
  "ANXIOUS",
  "SAD",
  "ANGRY",
]

export function EmotionSelector({ value, onChange, className }: EmotionSelectorProps) {
  return (
    <div className={cn("space-y-3", className)}>
      <p className="text-sm font-medium text-foreground">오늘의 기분은 어땠나요?</p>
      <div className="flex flex-wrap gap-2">
        {emotions.map((emotion) => {
          const meta = EMOTION_META[emotion]
          const isSelected = value === emotion
          
          return (
            <button
              key={emotion}
              onClick={() => onChange(emotion)}
              className={cn(
                "flex items-center gap-1.5 px-3 py-2 rounded-full border transition-all",
                "text-sm font-medium",
                isSelected
                  ? `${meta.bgColor} ${meta.color} border-current shadow-sm scale-105`
                  : "bg-card border-border text-muted-foreground hover:border-primary/30"
              )}
            >
              <span className="text-base">{meta.weatherIcon}</span>
              <span>{meta.label}</span>
            </button>
          )
        })}
      </div>
    </div>
  )
}
