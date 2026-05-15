"use client"

import { Flame } from "lucide-react"
import { cn } from "@/lib/utils"
import { getStreakLevel, STREAK_LEVELS } from "@/types/diary"

interface StreakBadgeProps {
  currentStreak: number
  longestStreak?: number
  showDetails?: boolean
  className?: string
}

export function StreakBadge({ 
  currentStreak, 
  longestStreak,
  showDetails = false,
  className 
}: StreakBadgeProps) {
  const level = getStreakLevel(currentStreak)
  const nextLevel = STREAK_LEVELS.find((l) => l.days > currentStreak)

  return (
    <div className={cn("space-y-2", className)}>
      {/* Main badge */}
      <div className={cn(
        "inline-flex items-center gap-2 px-3 py-2 rounded-xl",
        "bg-gradient-to-r from-amber-50 to-orange-50 border border-amber-200/50"
      )}>
        <div className="flex items-center gap-1">
          {level ? (
            <span className="text-xl">{level.emoji}</span>
          ) : (
            <Flame className="w-5 h-5 text-orange-500" />
          )}
          <span className="font-bold text-orange-600">{currentStreak}</span>
          <span className="text-sm text-orange-600/80">일 연속</span>
        </div>
      </div>

      {/* Details */}
      {showDetails && (
        <div className="space-y-2">
          {/* Progress to next level */}
          {nextLevel && (
            <div className="space-y-1">
              <div className="flex items-center justify-between text-xs text-muted-foreground">
                <span>다음 레벨: {nextLevel.emoji} {nextLevel.label}</span>
                <span>{currentStreak}/{nextLevel.days}일</span>
              </div>
              <div className="h-1.5 bg-muted rounded-full overflow-hidden">
                <div 
                  className="h-full bg-gradient-to-r from-amber-400 to-orange-400 rounded-full transition-all"
                  style={{ width: `${(currentStreak / nextLevel.days) * 100}%` }}
                />
              </div>
            </div>
          )}

          {/* Longest streak */}
          {longestStreak !== undefined && longestStreak > 0 && (
            <p className="text-xs text-muted-foreground">
              최고 기록: {longestStreak}일 연속
            </p>
          )}
        </div>
      )}
    </div>
  )
}
