"use client"

import { useState } from "react"
import { Plus, MapPin, Clock, GraduationCap, Coffee, Users } from "lucide-react"
import { cn } from "@/lib/utils"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import type { PlanEntry, EntryType } from "@/types/planner"
import { ENTRY_TYPE_LABELS, ENTRY_TYPE_COLORS } from "@/types/planner"

interface DailyTimelineProps {
  date: Date
  entries: PlanEntry[]
  onAddEntry?: () => void
  onEditEntry?: (entry: PlanEntry) => void
  className?: string
}

const ENTRY_ICONS: Record<EntryType, React.ReactNode> = {
  CLASS: <GraduationCap className="w-4 h-4" />,
  FREE: <Coffee className="w-4 h-4" />,
  ACTIVITY: <Users className="w-4 h-4" />,
}

function formatTime(time: string) {
  const [hour, minute] = time.split(":")
  const h = parseInt(hour)
  return `${h < 12 ? "오전" : "오후"} ${h > 12 ? h - 12 : h}:${minute}`
}

export function DailyTimeline({ 
  date, 
  entries, 
  onAddEntry,
  onEditEntry,
  className 
}: DailyTimelineProps) {
  const sortedEntries = [...entries].sort((a, b) => 
    a.startTime.localeCompare(b.startTime)
  )

  const formatDateHeader = (d: Date) => {
    const month = d.getMonth() + 1
    const day = d.getDate()
    const weekday = ["일", "월", "화", "수", "목", "금", "토"][d.getDay()]
    return `${month}월 ${day}일 ${weekday}요일`
  }

  return (
    <div className={cn("space-y-4", className)}>
      {/* Date Header */}
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-foreground">
          {formatDateHeader(date)}
        </h2>
        <Button
          variant="ghost"
          size="sm"
          onClick={onAddEntry}
          className="text-primary"
        >
          <Plus className="w-4 h-4 mr-1" />
          일정 추가
        </Button>
      </div>

      {/* Timeline */}
      <div className="relative">
        {/* Timeline line */}
        <div className="absolute left-[18px] top-0 bottom-0 w-0.5 bg-border" />

        {/* Entries */}
        <div className="space-y-3">
          {sortedEntries.length === 0 ? (
            <Card className="ml-10 bg-muted/50">
              <CardContent className="py-8 text-center">
                <p className="text-muted-foreground text-sm">
                  등록된 일정이 없어요
                </p>
                <Button
                  variant="link"
                  size="sm"
                  onClick={onAddEntry}
                  className="text-primary mt-2"
                >
                  첫 일정을 추가해보세요
                </Button>
              </CardContent>
            </Card>
          ) : (
            sortedEntries.map((entry) => (
              <div key={entry.id} className="flex gap-3">
                {/* Time indicator */}
                <div className="flex flex-col items-center">
                  <div className={cn(
                    "w-9 h-9 rounded-full flex items-center justify-center border-2 bg-card z-10",
                    ENTRY_TYPE_COLORS[entry.type]
                  )}>
                    {ENTRY_ICONS[entry.type]}
                  </div>
                </div>

                {/* Entry card */}
                <Card 
                  className={cn(
                    "flex-1 cursor-pointer transition-shadow hover:shadow-md border",
                    ENTRY_TYPE_COLORS[entry.type].split(" ").slice(0, 2).join(" ")
                  )}
                  onClick={() => onEditEntry?.(entry)}
                >
                  <CardContent className="py-3 px-4">
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-foreground truncate">
                          {entry.activity}
                        </p>
                        <div className="flex items-center gap-3 mt-1.5 text-xs text-muted-foreground">
                          <span className="flex items-center gap-1">
                            <Clock className="w-3 h-3" />
                            {formatTime(entry.startTime)} - {formatTime(entry.endTime)}
                          </span>
                          {entry.location && (
                            <span className="flex items-center gap-1">
                              <MapPin className="w-3 h-3" />
                              {entry.location}
                            </span>
                          )}
                        </div>
                      </div>
                      <span className={cn(
                        "text-xs px-2 py-0.5 rounded-full shrink-0",
                        ENTRY_TYPE_COLORS[entry.type]
                      )}>
                        {ENTRY_TYPE_LABELS[entry.type]}
                      </span>
                    </div>
                  </CardContent>
                </Card>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}
