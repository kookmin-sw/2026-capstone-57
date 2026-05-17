"use client"

import { MapPin, Clock, ChevronRight } from "lucide-react"
import { Badge } from "@/components/badge"
import { cn } from "@/lib/utils"

interface MatchForecastCardProps {
  className?: string
}

export function MatchForecastCard({ className }: MatchForecastCardProps) {
  return (
    <div className={cn(
      "bg-card rounded-2xl p-4 shadow-sm border border-border/50",
      "active:scale-[0.98] transition-transform cursor-pointer",
      className
    )}>
      <div className="flex items-start gap-3">
        {/* Avatar placeholder */}
        <div className="size-14 rounded-xl bg-gradient-to-br from-secondary/50 to-accent/30 flex items-center justify-center shrink-0">
          <span className="text-2xl">🌤️</span>
        </div>
        
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <Badge variant="secondary" size="sm">오늘의 스침</Badge>
            <Badge variant="accent" size="sm">NEW</Badge>
          </div>
          
          <h3 className="font-semibold text-foreground mb-1.5 text-balance">
            오늘, 같은 길을 스친 사람이 있어요
          </h3>
          
          <div className="flex items-center gap-3 text-xs text-muted-foreground">
            <span className="flex items-center gap-1">
              <MapPin className="size-3" />
              중앙도서관 근처
            </span>
            <span className="flex items-center gap-1">
              <Clock className="size-3" />
              오전 10:30
            </span>
          </div>
        </div>
        
        <ChevronRight className="size-5 text-muted-foreground shrink-0 mt-4" />
      </div>
    </div>
  )
}
