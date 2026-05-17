"use client"

import { cn } from "@/lib/utils"
import type { MatchPartner } from "@/types/match"

interface MatchHeroCardProps {
  partner: MatchPartner
  intimacy: number
  className?: string
}

export function MatchHeroCard({ partner, intimacy, className }: MatchHeroCardProps) {
  return (
    <div className={cn(
      "relative overflow-hidden rounded-2xl px-4 py-3",
      "bg-gradient-to-br from-amber-100 via-yellow-100 to-orange-100 shadow-sm",
      className
    )}>
      <div className="relative flex items-center gap-3">
        {/* Avatar */}
        <div className="w-11 h-11 rounded-xl gradient-gem flex items-center justify-center shadow-gem shrink-0">
          <span className="text-xl">{partner.profileEmoji}</span>
        </div>

        {/* Info */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <h2 className="text-base font-semibold text-foreground">
              {partner.nickname}
            </h2>
            <span className="text-[11px] text-muted-foreground">
              {partner.department} · {partner.studentYear}
            </span>
          </div>
          {/* Hobby tags + intimacy in one row */}
          <div className="flex items-center justify-between mt-1">
            <div className="flex gap-1">
              {partner.hobbies.slice(0, 3).map((hobby) => (
                <span
                  key={hobby}
                  className="px-1.5 py-0.5 rounded-full bg-secondary text-secondary-foreground text-[9px]"
                >
                  #{hobby}
                </span>
              ))}
            </div>
            <span className="text-[11px] font-semibold text-foreground shrink-0">{intimacy}%</span>
          </div>
        </div>
      </div>

      {/* Intimacy progress bar */}
      <div className="mt-2">
        <div className="h-1.5 rounded-full bg-background/40 overflow-hidden">
          <div
            className="h-full rounded-full gradient-gem transition-all duration-500"
            style={{ width: `${intimacy}%` }}
          />
        </div>
      </div>
    </div>
  )
}
