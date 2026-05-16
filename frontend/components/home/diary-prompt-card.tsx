"use client"

import { useRouter } from "next/navigation"
import { Sparkles, PenLine, Flame } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

interface DiaryPromptCardProps {
  currentStreak?: number
  className?: string
}

export function DiaryPromptCard({ currentStreak = 5, className }: DiaryPromptCardProps) {
  const router = useRouter()

  return (
    <div className={cn(
      "bg-gradient-to-br from-secondary/30 to-secondary/10 rounded-2xl p-5",
      "border border-secondary/30",
      className
    )}>
      <div className="flex items-start gap-4">
        <div className="size-12 rounded-xl bg-secondary/50 flex items-center justify-center shrink-0">
          <PenLine className="size-6 text-foreground/70" />
        </div>
        
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-1">
            <div className="flex items-center gap-1.5">
              <Sparkles className="size-4 text-accent" />
              <span className="text-xs font-medium text-accent">오늘의 일기</span>
            </div>
            {currentStreak > 0 && (
              <div className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-amber-100 text-amber-600 text-xs font-medium">
                <Flame className="size-3" />
                {currentStreak}일 연속
              </div>
            )}
          </div>
          
          <h3 className="font-semibold text-foreground mb-2 text-balance">
            오늘 하루 중 가장 기억에 남는 순간은 언제였나요?
          </h3>
          
          <p className="text-sm text-muted-foreground mb-4">
            일기를 쓰면 비슷한 감성을 가진 사람을 만날 수 있어요
          </p>
          
          <Button 
            size="sm" 
            className="bg-secondary text-secondary-foreground hover:bg-secondary/80"
            onClick={() => router.push("/diary")}
          >
            오늘의 일기 쓰기
          </Button>
        </div>
      </div>
    </div>
  )
}
