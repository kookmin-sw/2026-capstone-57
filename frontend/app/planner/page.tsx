"use client"

import { useState } from "react"
import { ChevronLeft, ChevronRight, Calendar, Grid3X3 } from "lucide-react"
import { AppShell } from "@/components/app-shell"
import { Button } from "@/components/ui/button"
import { DailyTimeline } from "@/components/planner/daily-timeline"
import { TimetableGrid } from "@/components/planner/timetable-grid"
import type { PlanEntry, TimetableEntry } from "@/types/planner"
import { cn } from "@/lib/utils"

type ViewMode = "daily" | "weekly"

// Sample data
const sampleTimetable: TimetableEntry[] = [
  {
    id: "1",
    userId: "user-1",
    dayOfWeek: 1,
    startTime: "09:00",
    endTime: "10:30",
    location: "공학관 401",
    courseName: "데이터베이스",
  },
  {
    id: "2",
    userId: "user-1",
    dayOfWeek: 1,
    startTime: "13:00",
    endTime: "14:30",
    location: "인문관 201",
    courseName: "심리학개론",
  },
  {
    id: "3",
    userId: "user-1",
    dayOfWeek: 2,
    startTime: "10:30",
    endTime: "12:00",
    location: "경영관 302",
    courseName: "마케팅원론",
  },
  {
    id: "4",
    userId: "user-1",
    dayOfWeek: 3,
    startTime: "09:00",
    endTime: "10:30",
    location: "공학관 401",
    courseName: "데이터베이스",
  },
  {
    id: "5",
    userId: "user-1",
    dayOfWeek: 4,
    startTime: "14:00",
    endTime: "15:30",
    location: "도서관 세미나실",
    courseName: "캡스톤디자인",
  },
  {
    id: "6",
    userId: "user-1",
    dayOfWeek: 5,
    startTime: "11:00",
    endTime: "12:30",
    location: "학생회관",
    courseName: "창업실습",
  },
]

const sampleDailyEntries: PlanEntry[] = [
  {
    id: "1",
    startTime: "09:00",
    endTime: "10:30",
    location: "공학관 401",
    activity: "데이터베이스 수업",
    type: "CLASS",
  },
  {
    id: "2",
    startTime: "12:00",
    endTime: "13:00",
    location: "학생식당",
    activity: "점심 식사",
    type: "FREE",
  },
  {
    id: "3",
    startTime: "13:00",
    endTime: "14:30",
    location: "인문관 201",
    activity: "심리학개론 수업",
    type: "CLASS",
  },
  {
    id: "4",
    startTime: "15:00",
    endTime: "17:00",
    location: "도서관",
    activity: "스터디 모임",
    type: "ACTIVITY",
  },
]

export default function PlannerPage() {
  const [viewMode, setViewMode] = useState<ViewMode>("daily")
  const [selectedDate, setSelectedDate] = useState(new Date())

  const goToPrevDay = () => {
    const prev = new Date(selectedDate)
    prev.setDate(prev.getDate() - 1)
    setSelectedDate(prev)
  }

  const goToNextDay = () => {
    const next = new Date(selectedDate)
    next.setDate(next.getDate() + 1)
    setSelectedDate(next)
  }

  const goToToday = () => {
    setSelectedDate(new Date())
  }

  return (
    <AppShell title="플래너">
      <div className="p-4 space-y-4">
        {/* View Toggle & Date Navigation */}
        <div className="flex items-center justify-between">
          {/* View Mode Toggle */}
          <div className="flex bg-muted rounded-lg p-1">
            <button
              onClick={() => setViewMode("daily")}
              className={cn(
                "flex items-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium transition-colors",
                viewMode === "daily"
                  ? "bg-card text-foreground shadow-sm"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              <Calendar className="w-4 h-4" />
              일간
            </button>
            <button
              onClick={() => setViewMode("weekly")}
              className={cn(
                "flex items-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium transition-colors",
                viewMode === "weekly"
                  ? "bg-card text-foreground shadow-sm"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              <Grid3X3 className="w-4 h-4" />
              주간
            </button>
          </div>

          {/* Date Navigation (only for daily view) */}
          {viewMode === "daily" && (
            <div className="flex items-center gap-1">
              <Button variant="ghost" size="icon" onClick={goToPrevDay}>
                <ChevronLeft className="w-4 h-4" />
              </Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={goToToday}
                className="text-xs"
              >
                오늘
              </Button>
              <Button variant="ghost" size="icon" onClick={goToNextDay}>
                <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          )}
        </div>

        {/* Content */}
        {viewMode === "daily" ? (
          <DailyTimeline
            date={selectedDate}
            entries={sampleDailyEntries}
            onAddEntry={() => {}}
            onEditEntry={() => {}}
          />
        ) : (
          <TimetableGrid
            entries={sampleTimetable}
            onAddEntry={() => {}}
            onEditEntry={() => {}}
            onDeleteEntry={() => {}}
          />
        )}
      </div>
    </AppShell>
  )
}
