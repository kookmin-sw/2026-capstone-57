"use client"

import { ChevronLeft, Bell } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

interface TopHeaderProps {
  title?: string
  showBackButton?: boolean
  rightAction?: React.ReactNode
  className?: string
}

export function TopHeader({ 
  title, 
  showBackButton = false, 
  rightAction,
  className 
}: TopHeaderProps) {
  return (
    <header className={cn(
      "sticky top-0 z-50 bg-background/95 backdrop-blur-sm",
      "px-4 py-3 flex items-center justify-between",
      "border-b border-border/50",
      className
    )}>
      <div className="flex items-center gap-2 min-w-[40px]">
        {showBackButton && (
          <Button variant="ghost" size="icon" className="text-foreground -ml-2">
            <ChevronLeft className="size-6" />
          </Button>
        )}
      </div>
      
      {title ? (
        <h1 className="text-lg font-semibold text-foreground absolute left-1/2 -translate-x-1/2">
          {title}
        </h1>
      ) : (
        <div className="flex items-center gap-1.5">
          <span className="text-xl font-bold text-primary">일기예보</span>
          <span className="text-secondary text-lg">☀️</span>
        </div>
      )}
      
      <div className="flex items-center gap-1 min-w-[40px] justify-end">
        {rightAction || (
          <Button variant="ghost" size="icon" className="text-muted-foreground">
            <Bell className="size-5" />
          </Button>
        )}
      </div>
    </header>
  )
}
