"use client"

import { useState, useMemo } from "react"
import { useRouter } from "next/navigation"
import { ChevronLeft, ChevronRight, Cloud } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import type { DiaryEntry } from "@/types/diary"
import { EMOTION_META } from "@/types/diary"

interface DiaryCalendarProps {
  entries: DiaryEntry[]
  className?: string
}

// Calendar helpers
function getDaysInMonth(year: number, month: number) {
  return new Date(year, month + 1, 0).getDate()
}

function getFirstDayOfMonth(year: number, month: number) {
  return new Date(year, month, 1).getDay()
}

function formatDateKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`
}

function isSameDay(d1: Date, d2: Date) {
  return d1.toDateString() === d2.toDateString()
}

function isFutureDate(date: Date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const checkDate = new Date(date)
  checkDate.setHours(0, 0, 0, 0)
  return checkDate > today
}

function isPastDate(date: Date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const checkDate = new Date(date)
  checkDate.setHours(0, 0, 0, 0)
  return checkDate < today
}

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"]

export function DiaryCalendar({ entries, className }: DiaryCalendarProps) {
  const router = useRouter()
  const [currentMonth, setCurrentMonth] = useState(new Date())
  const [shakingDate, setShakingDate] = useState<number | null>(null)
  const today = new Date()

  // Map entries by date
  const entryByDate = useMemo(() => {
    const map = new Map<string, DiaryEntry>()
    entries.forEach((entry) => {
      map.set(entry.entryDate, entry)
    })
    return map
  }, [entries])

  // Generate calendar grid
  const calendarWeeks = useMemo(() => {
    const year = currentMonth.getFullYear()
    const month = currentMonth.getMonth()
    const daysInMonth = getDaysInMonth(year, month)
    const firstDay = getFirstDayOfMonth(year, month)
    
    const weeks: (number | null)[][] = []
    let currentWeek: (number | null)[] = Array(firstDay).fill(null)

    for (let day = 1; day <= daysInMonth; day++) {
      currentWeek.push(day)
      if (currentWeek.length === 7) {
        weeks.push(currentWeek)
        currentWeek = []
      }
    }
    
    if (currentWeek.length > 0) {
      while (currentWeek.length < 7) currentWeek.push(null)
      weeks.push(currentWeek)
    }

    return weeks
  }, [currentMonth])

  const goToPreviousMonth = () => {
    setCurrentMonth((prev) => new Date(prev.getFullYear(), prev.getMonth() - 1, 1))
  }

  const goToNextMonth = () => {
    setCurrentMonth((prev) => new Date(prev.getFullYear(), prev.getMonth() + 1, 1))
  }

  const handleDateClick = (day: number) => {
    const clickedDate = new Date(currentMonth.getFullYear(), currentMonth.getMonth(), day)
    
    if (isFutureDate(clickedDate)) {
      // Shake animation for future dates
      setShakingDate(day)
      setTimeout(() => setShakingDate(null), 500)
      return
    }

    // Navigate to diary detail page
    const dateStr = formatDateKey(clickedDate)
    router.push(`/diary/${dateStr}`)
  }

  return (
    <div className={cn("flex flex-col", className)}>
      {/* Month Navigation - Compact */}
      <div className="flex items-center justify-between mb-3">
        <Button
          variant="ghost"
          size="icon"
          className="size-8 rounded-full hover:bg-secondary/50"
          onClick={goToPreviousMonth}
        >
          <ChevronLeft className="w-4 h-4" />
        </Button>
        
        <h3 className="font-semibold text-base text-foreground">
          {currentMonth.getFullYear()}년 {currentMonth.getMonth() + 1}월
        </h3>
        
        <Button
          variant="ghost"
          size="icon"
          className="size-8 rounded-full hover:bg-secondary/50"
          onClick={goToNextMonth}
        >
          <ChevronRight className="w-4 h-4" />
        </Button>
      </div>

      {/* Full Calendar - No Card wrapper for full-screen feel */}
      <div className="flex-1 flex flex-col bg-card rounded-2xl p-3 shadow-sm border border-border/30">
        {/* Weekday Headers - Compact */}
        <div className="grid grid-cols-7 mb-2">
          {WEEKDAYS.map((day, idx) => (
            <div 
              key={day} 
              className={cn(
                "text-center text-[11px] font-medium py-1",
                idx === 0 ? "text-red-400" : idx === 6 ? "text-blue-400" : "text-muted-foreground"
              )}
            >
              {day}
            </div>
          ))}
        </div>

        {/* Days Grid - Flex grow to fill space */}
        <div className="flex-1 grid grid-rows-6 gap-1">
          {calendarWeeks.map((week, weekIdx) => (
            <div key={weekIdx} className="grid grid-cols-7 gap-1">
              {week.map((day, dayIdx) => {
                if (day === null) {
                  return <div key={dayIdx} className="rounded-lg" />
                }

                const cellDate = new Date(currentMonth.getFullYear(), currentMonth.getMonth(), day)
                const dateKey = formatDateKey(cellDate)
                const entry = entryByDate.get(dateKey)
                const isToday = isSameDay(cellDate, today)
                const isFuture = isFutureDate(cellDate)
                const isPast = isPastDate(cellDate)
                const isPastUnwritten = isPast && !entry
                const isShaking = shakingDate === day

                return (
                  <button
                    key={dayIdx}
                    onClick={() => handleDateClick(day)}
                    className={cn(
                      "rounded-lg flex flex-col items-center justify-center gap-0.5 transition-all duration-200",
                      "relative h-full",
                      // Future dates - slightly faded but warm
                      isFuture && "opacity-80 cursor-not-allowed",
                      // Past and today - interactive
                      !isFuture && "hover:bg-secondary/30 active:scale-95",
                      // Entry with emotion - pastel card feeling
                      entry && cn(EMOTION_META[entry.emotionTag].bgColor, "shadow-sm"),
                      // Today special glow
                      isToday && "ring-2 ring-primary/50 shadow-md",
                      // Shake animation
                      isShaking && "animate-shake"
                    )}
                  >
                    {/* Today Badge */}
                    {isToday && (
                      <div className="absolute -top-1 left-1/2 -translate-x-1/2 z-10">
                        <span className="text-[7px] font-bold text-primary-foreground bg-primary px-1 py-0.5 rounded-full shadow-sm">
                          TODAY
                        </span>
                      </div>
                    )}
                    
                    {/* Day Number */}
                    <span className={cn(
                      "text-xs leading-none",
                      isToday && "font-bold text-primary",
                      // Future - soft muted but not grey
                      isFuture && "text-muted-foreground/70",
                      // Past unwritten - normal color
                      isPastUnwritten && "text-foreground/80",
                      // Weekend colors (only when not special states)
                      dayIdx === 0 && !isFuture && !isToday && "text-red-400",
                      dayIdx === 6 && !isFuture && !isToday && "text-blue-400"
                    )}>
                      {day}
                    </span>
                    
                    {/* Emotion Weather Icon - for written entries */}
                    {entry && (
                      <span className="text-xs leading-none">
                        {EMOTION_META[entry.emotionTag].weatherIcon}
                      </span>
                    )}

                    {/* Past unwritten indicator - small outline cloud */}
                    {isPastUnwritten && (
                      <Cloud className="w-2.5 h-2.5 text-muted-foreground/40 stroke-[1.5]" />
                    )}

                    {/* Future date - subtle dot indicator */}
                    {isFuture && !isToday && (
                      <span className="w-1 h-1 rounded-full bg-muted-foreground/30" />
                    )}
                  </button>
                )
              })}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
