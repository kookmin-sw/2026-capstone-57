"use client"

import { cn } from "@/lib/utils"

interface WeatherStatusCardProps {
  userName?: string
  className?: string
}

function formatDate() {
  const today = new Date()
  const month = today.getMonth() + 1
  const date = today.getDate()
  const days = ["일", "월", "화", "수", "목", "금", "토"]
  const day = days[today.getDay()]
  return `${month}월 ${date}일 ${day}요일`
}

export function WeatherStatusCard({ userName = "아리", className }: WeatherStatusCardProps) {
  return (
    <div className={cn(
      "relative overflow-hidden rounded-xl px-4 py-3",
      "bg-gradient-to-r from-primary to-primary/80",
      className
    )}>
      <div className="flex items-center justify-between mb-2">
        <p className="text-white/70 text-xs">{formatDate()}</p>
        <span className="text-base">☀️</span>
      </div>
      <h2 className="text-white font-semibold mb-1">안녕하세요, {userName}님</h2>
      <p className="text-white/80 text-sm">
        오늘은 좋은 인연을 만나기 딱 좋은 날이에요
      </p>
    </div>
  )
}
