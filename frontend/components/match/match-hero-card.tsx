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
      "relative overflow-hidden rounded-3xl p-5",
      "gradient-aurora shadow-glow",
      className
    )}>
      {/* Background decoration */}
      <div className="absolute top-4 right-4 w-24 h-24 rounded-full bg-white/10 blur-2xl" />
      
      <div className="relative flex gap-4">
        {/* Avatar */}
        <div className="w-20 h-20 rounded-3xl gradient-gem flex items-center justify-center shadow-gem animate-float shrink-0">
          <span className="text-3xl">{partner.profileEmoji}</span>
        </div>
        
        {/* Info */}
        <div className="flex-1 min-w-0">
          <h2 className="text-xl font-semibold text-foreground mb-0.5">
            {partner.nickname}
          </h2>
          <p className="text-xs text-muted-foreground mb-2">
            {partner.department} · {partner.studentYear}
          </p>
          
          {/* Hobby tags */}
          <div className="flex flex-wrap gap-1">
            {partner.hobbies.slice(0, 3).map((hobby) => (
              <span 
                key={hobby}
                className="px-2 py-0.5 rounded-full bg-secondary text-secondary-foreground text-[10px]"
              >
                #{hobby}
              </span>
            ))}
          </div>
        </div>
      </div>
      
      {/* Intimacy progress */}
      <div className="mt-4">
        <div className="flex justify-between items-center mb-1.5">
          <span className="text-xs text-foreground/80">친밀도</span>
          <span className="text-xs font-semibold text-foreground">{intimacy}%</span>
        </div>
        <div className="h-2 rounded-full bg-background/40 overflow-hidden">
          <div 
            className="h-full rounded-full gradient-gem transition-all duration-500"
            style={{ width: `${intimacy}%` }}
          />
        </div>
      </div>
    </div>
  )
}
