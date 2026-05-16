"use client"

import { CloudSun } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"
import { cn } from "@/lib/utils"

interface FutureDateMessageProps {
  date: Date
  className?: string
}

function formatDate(date: Date) {
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}월 ${day}일`
}

export function FutureDateMessage({ date, className }: FutureDateMessageProps) {
  return (
    <Card className={cn(
      "border-border/50 shadow-sm overflow-hidden",
      "animate-in fade-in slide-in-from-bottom-4 duration-300",
      className
    )}>
      <CardContent className="p-8 text-center">
        <div className="flex flex-col items-center gap-4">
          <div className="size-16 rounded-full bg-muted/50 flex items-center justify-center">
            <CloudSun className="size-8 text-muted-foreground/50" />
          </div>
          
          <div className="space-y-2">
            <h3 className="font-semibold text-foreground">
              아직 오지 않은 하루예요
            </h3>
            <p className="text-sm text-muted-foreground">
              {formatDate(date)}의 일기는 아직 작성할 수 없어요
            </p>
          </div>

          <p className="text-xs text-muted-foreground/70">
            그날이 되면 이곳에 일기를 작성할 수 있어요
          </p>
        </div>
      </CardContent>
    </Card>
  )
}
