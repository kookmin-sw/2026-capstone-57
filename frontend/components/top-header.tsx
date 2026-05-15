"use client"

import Image from "next/image"
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
  const isHome = !title

  return (
    <header className={cn(
      "sticky top-0 z-50 bg-background/95 backdrop-blur-sm",
      "px-4 flex items-center justify-between",
      "border-b border-border/50",
      isHome ? "h-[116px]" : "h-[48px]",
      className
    )}>
      <div className="flex items-center gap-2 min-w-[40px]">
        {showBackButton && (
          <Button variant="ghost" size="icon" className="text-foreground -ml-2">
            <ChevronLeft className="size-6" />
          </Button>
        )}
      </div>

      {/* Center: logo */}
      <div className="absolute left-1/2 -translate-x-1/2">
        {isHome ? (
          <Image src="/logo.png" alt="일기예보" width={116} height={116} className="h-[100px] w-auto" />
        ) : (
          <Image src="/logo.png" alt="일기예보" width={48} height={48} className="h-[38px] w-auto" />
        )}
      </div>

      {/* Right side */}
      <div className="flex items-center gap-1 min-w-[40px] justify-end">
        {isHome ? (
          rightAction || (
            <Button variant="ghost" size="icon" className="text-muted-foreground">
              <Bell className="size-5" />
            </Button>
          )
        ) : (
          <span className="text-sm font-semibold text-foreground">{title}</span>
        )}
      </div>
    </header>
  )
}
