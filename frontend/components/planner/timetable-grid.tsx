"use client"

import { useState } from "react"
import { Plus, X } from "lucide-react"
import { cn } from "@/lib/utils"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import type { TimetableEntry, DayOfWeek } from "@/types/planner"
import { DAY_LABELS, COURSE_COLORS } from "@/types/planner"

interface TimetableGridProps {
  entries: TimetableEntry[]
  onAddEntry?: (dayOfWeek: DayOfWeek, hour: number) => void
  onEditEntry?: (entry: TimetableEntry) => void
  onDeleteEntry?: (entryId: string) => void
  className?: string
}

const HOURS = Array.from({ length: 12 }, (_, i) => i + 9) // 9AM to 8PM
const DAYS: DayOfWeek[] = [1, 2, 3, 4, 5] // Mon to Fri

function parseTime(time: string): number {
  const [hour, minute] = time.split(":").map(Number)
  return hour + minute / 60
}

function getEntryStyle(entry: TimetableEntry) {
  const startHour = parseTime(entry.startTime)
  const endHour = parseTime(entry.endTime)
  const top = (startHour - 9) * 48 // 48px per hour
  const height = (endHour - startHour) * 48
  return { top: `${top}px`, height: `${height}px` }
}

export function TimetableGrid({
  entries,
  onAddEntry,
  onEditEntry,
  onDeleteEntry,
  className,
}: TimetableGridProps) {
  const [hoveredCell, setHoveredCell] = useState<{ day: DayOfWeek; hour: number } | null>(null)

  // Group entries by day
  const entriesByDay = DAYS.reduce((acc, day) => {
    acc[day] = entries.filter((e) => e.dayOfWeek === day)
    return acc
  }, {} as Record<DayOfWeek, TimetableEntry[]>)

  // Assign colors to courses
  const courseColors = new Map<string, string>()
  entries.forEach((entry, index) => {
    if (!courseColors.has(entry.courseName)) {
      courseColors.set(entry.courseName, COURSE_COLORS[courseColors.size % COURSE_COLORS.length])
    }
  })

  return (
    <Card className={cn("overflow-hidden", className)}>
      <CardHeader className="pb-2">
        <CardTitle className="text-base">주간 시간표</CardTitle>
      </CardHeader>
      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <div className="min-w-[400px]">
            {/* Header */}
            <div className="grid grid-cols-[48px_repeat(5,1fr)] border-b border-border">
              <div className="p-2 text-center text-xs text-muted-foreground" />
              {DAYS.map((day) => (
                <div
                  key={day}
                  className="p-2 text-center text-sm font-medium text-foreground border-l border-border"
                >
                  {DAY_LABELS[day]}
                </div>
              ))}
            </div>

            {/* Grid */}
            <div className="grid grid-cols-[48px_repeat(5,1fr)]">
              {/* Time labels */}
              <div className="relative">
                {HOURS.map((hour) => (
                  <div
                    key={hour}
                    className="h-12 border-b border-border flex items-start justify-center pt-1"
                  >
                    <span className="text-[10px] text-muted-foreground">
                      {hour > 12 ? hour - 12 : hour}{hour >= 12 ? "PM" : "AM"}
                    </span>
                  </div>
                ))}
              </div>

              {/* Day columns */}
              {DAYS.map((day) => (
                <div key={day} className="relative border-l border-border">
                  {/* Hour cells */}
                  {HOURS.map((hour) => (
                    <div
                      key={hour}
                      className={cn(
                        "h-12 border-b border-border transition-colors cursor-pointer",
                        hoveredCell?.day === day && hoveredCell?.hour === hour && "bg-primary/5"
                      )}
                      onMouseEnter={() => setHoveredCell({ day, hour })}
                      onMouseLeave={() => setHoveredCell(null)}
                      onClick={() => onAddEntry?.(day, hour)}
                    >
                      {hoveredCell?.day === day && hoveredCell?.hour === hour && (
                        <div className="w-full h-full flex items-center justify-center">
                          <Plus className="w-4 h-4 text-primary/50" />
                        </div>
                      )}
                    </div>
                  ))}

                  {/* Entries */}
                  {entriesByDay[day]?.map((entry) => {
                    const style = getEntryStyle(entry)
                    const colorClass = courseColors.get(entry.courseName) || COURSE_COLORS[0]
                    
                    return (
                      <div
                        key={entry.id}
                        className={cn(
                          "absolute left-0.5 right-0.5 rounded-md border p-1 cursor-pointer",
                          "overflow-hidden transition-shadow hover:shadow-md",
                          colorClass
                        )}
                        style={style}
                        onClick={(e) => {
                          e.stopPropagation()
                          onEditEntry?.(entry)
                        }}
                      >
                        <div className="flex items-start justify-between gap-1">
                          <div className="min-w-0 flex-1">
                            <p className="text-[10px] font-medium truncate">
                              {entry.courseName}
                            </p>
                            <p className="text-[9px] opacity-75 truncate">
                              {entry.location}
                            </p>
                          </div>
                          {onDeleteEntry && (
                            <button
                              onClick={(e) => {
                                e.stopPropagation()
                                onDeleteEntry(entry.id)
                              }}
                              className="p-0.5 rounded hover:bg-black/10 shrink-0"
                            >
                              <X className="w-3 h-3" />
                            </button>
                          )}
                        </div>
                      </div>
                    )
                  })}
                </div>
              ))}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
