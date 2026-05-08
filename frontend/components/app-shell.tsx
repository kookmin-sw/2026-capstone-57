"use client"

import { TopHeader } from "@/components/top-header"
import { BottomNav } from "@/components/bottom-nav"

interface AppShellProps {
  children: React.ReactNode
  title?: string
  showBackButton?: boolean
  rightAction?: React.ReactNode
}

export function AppShell({ 
  children, 
  title,
  showBackButton = false,
  rightAction 
}: AppShellProps) {
  return (
    <div className="h-screen bg-muted flex justify-center overflow-hidden">
      <div className="w-full max-w-[430px] h-full bg-background flex flex-col relative shadow-xl">
        <TopHeader 
          title={title} 
          showBackButton={showBackButton}
          rightAction={rightAction}
        />
        <main className="flex-1 pb-20 overflow-y-auto scrollbar-hide">
          {children}
        </main>
        <BottomNav />
      </div>
    </div>
  )
}
