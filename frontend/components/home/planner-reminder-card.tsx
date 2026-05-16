"use client"

import { useRouter } from "next/navigation"
import { Calendar, Bell, ChevronRight } from "lucide-react"
import { Badge } from "@/components/badge"
import { cn } from "@/lib/utils"

interface PlannerReminderCardProps {
  upcomingCount?: number
  nextEvent?: {
    title: string
    time: string
  }
  className?: string
}

export function PlannerReminderCard({ 
  upcomingCount = 2,
  nextEvent = { title: "커피챗 with 지현님", time: "내일 오후 3시" },
  className 
}: PlannerReminderCardProps) {
  const router = useRouter()

  return (
    <div 
      className={cn(
        "bg-card rounded-2xl p-4 shadow-sm border border-border/50",
        "active:scale-[0.98] transition-transform cursor-pointer",
        className
      )}
      onClick={() => router.push("/planner")}
    >
      <div className="flex items-center gap-3">
        <div className="size-11 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
          <Calendar className="size-5 text-primary" />
        </div>
        
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-0.5">
            <h4 className="font-semibold text-foreground text-sm">다가오는 일정</h4>
            {upcomingCount > 0 && (
              <Badge variant="default" size="sm">{upcomingCount}개</Badge>
            )}
          </div>
          <p className="text-xs text-muted-foreground truncate">
            {nextEvent.time} &middot; {nextEvent.title}
          </p>
        </div>
        
        <div className="flex items-center gap-2 shrink-0">
          <div className="relative">
            <Bell className="size-5 text-muted-foreground" />
            {upcomingCount > 0 && (
              <span className="absolute -top-0.5 -right-0.5 size-2 bg-accent rounded-full" />
            )}
          </div>
          <ChevronRight className="size-5 text-muted-foreground" />
        </div>
      </div>
    </div>
  )
}
