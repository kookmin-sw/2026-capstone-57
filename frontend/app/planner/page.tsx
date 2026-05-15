"use client"

import { useState, useEffect, useCallback } from "react"
import { ChevronLeft, ChevronRight, Calendar, Grid3X3 } from "lucide-react"
import { AppShell } from "@/components/app-shell"
import { Button } from "@/components/ui/button"
import { DailyTimeline } from "@/components/planner/daily-timeline"
import { TimetableGrid } from "@/components/planner/timetable-grid"
import { AddEntryDrawer, type AddEntryFormData } from "@/components/planner/add-entry-drawer"
import {
  getPlanEntries,
  createPlanEntry,
  deletePlanEntry,
  formatDate,
  fromLocalTime,
  toLocalTime,
  type PlanEntryResponse,
} from "@/lib/api/planner"
import type { PlanEntry, TimetableEntry, DayOfWeek } from "@/types/planner"
import { cn } from "@/lib/utils"

type ViewMode = "daily" | "weekly"

// PlanEntryResponse → PlanEntry (일간 뷰용) 변환
function toPlanEntry(entry: PlanEntryResponse): PlanEntry {
  return {
    id: entry.id,
    startTime: fromLocalTime(entry.startTime),
    endTime: fromLocalTime(entry.endTime),
    location: entry.location || "",
    activity: entry.name || "",
    type: entry.type,
  }
}

// PlanEntryResponse → TimetableEntry (주간 뷰용) 변환
function toTimetableEntry(entry: PlanEntryResponse): TimetableEntry {
  const date = new Date(entry.date + "T00:00:00")
  // getDay(): 0=일, 1=월, ... 6=토
  const dayOfWeek = date.getDay() as DayOfWeek

  return {
    id: entry.id,
    userId: "",
    dayOfWeek,
    startTime: fromLocalTime(entry.startTime),
    endTime: fromLocalTime(entry.endTime),
    location: entry.location || "",
    courseName: entry.name || "",
    type: entry.type,
  }
}

export default function PlannerPage() {
  const [viewMode, setViewMode] = useState<ViewMode>("daily")
  const [selectedDate, setSelectedDate] = useState(new Date())
  const [dailyDrawerOpen, setDailyDrawerOpen] = useState(false)

  // API data
  const [dailyEntries, setDailyEntries] = useState<PlanEntry[]>([])
  const [weeklyEntries, setWeeklyEntries] = useState<TimetableEntry[]>([])
  const [loading, setLoading] = useState(false)

  // 일간 데이터 로드
  const loadDailyEntries = useCallback(async () => {
    try {
      setLoading(true)
      const dateStr = formatDate(selectedDate)
      const data = await getPlanEntries(dateStr)
      setDailyEntries(data.map(toPlanEntry))
    } catch (err) {
      console.error("일정 조회 실패:", err)
      setDailyEntries([])
    } finally {
      setLoading(false)
    }
  }, [selectedDate])

  // 주간 데이터 로드 (선택된 날짜가 포함된 주의 월~금)
  const loadWeeklyEntries = useCallback(async () => {
    try {
      setLoading(true)
      const day = selectedDate.getDay()
      const monday = new Date(selectedDate)
      monday.setDate(selectedDate.getDate() - (day === 0 ? 6 : day - 1))

      const allEntries: PlanEntryResponse[] = []
      for (let i = 0; i < 5; i++) {
        const d = new Date(monday)
        d.setDate(monday.getDate() + i)
        const data = await getPlanEntries(formatDate(d))
        allEntries.push(...data)
      }
      setWeeklyEntries(allEntries.map(toTimetableEntry))
    } catch (err) {
      console.error("주간 일정 조회 실패:", err)
      setWeeklyEntries([])
    } finally {
      setLoading(false)
    }
  }, [selectedDate])

  useEffect(() => {
    if (viewMode === "daily") {
      loadDailyEntries()
    } else {
      loadWeeklyEntries()
    }
  }, [viewMode, loadDailyEntries, loadWeeklyEntries])

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

  // 주간 뷰에서 일정 추가
  const handleAddEntry = async (entry: Omit<TimetableEntry, "id" | "userId">) => {
    try {
      // dayOfWeek로 날짜 계산
      const day = selectedDate.getDay()
      const monday = new Date(selectedDate)
      monday.setDate(selectedDate.getDate() - (day === 0 ? 6 : day - 1))
      const targetDate = new Date(monday)
      // dayOfWeek: 1=월, 2=화, ... 5=금
      targetDate.setDate(monday.getDate() + (entry.dayOfWeek - 1))

      await createPlanEntry({
        date: formatDate(targetDate),
        startTime: toLocalTime(entry.startTime),
        endTime: toLocalTime(entry.endTime),
        location: entry.location || undefined,
        name: entry.courseName,
        type: entry.type,
      })
      loadWeeklyEntries()
    } catch (err) {
      console.error("일정 추가 실패:", err)
    }
  }

  // 일간 뷰에서 일정 추가
  const handleDailyAddEntry = async (data: AddEntryFormData) => {
    if (!data.startTime || !data.endTime) {
      console.error("시작/종료 시간을 선택해주세요")
      return
    }
    const body = {
      date: formatDate(selectedDate),
      startTime: toLocalTime(data.startTime),
      endTime: toLocalTime(data.endTime),
      location: data.location || undefined,
      name: data.courseName || undefined,
      type: data.type,
    }
    console.log("일정 추가 요청:", JSON.stringify(body))
    try {
      await createPlanEntry(body)
      loadDailyEntries()
    } catch (err) {
      console.error("일정 추가 실패:", err)
    }
  }

  // 일정 삭제
  const handleDeleteEntry = async (entryId: string) => {
    try {
      await deletePlanEntry(entryId)
      if (viewMode === "daily") {
        loadDailyEntries()
      } else {
        loadWeeklyEntries()
      }
    } catch (err) {
      console.error("일정 삭제 실패:", err)
    }
  }

  return (
    <>
      <AppShell title="플래너" noScroll>
        <div className="p-4 h-full flex flex-col overflow-hidden">
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
          <div className={`flex-1 mt-3 ${viewMode === "weekly" ? "overflow-hidden" : "overflow-y-auto scrollbar-hide"}`}>
            {viewMode === "daily" ? (
              <DailyTimeline
                date={selectedDate}
                entries={dailyEntries}
                onAddEntry={() => setDailyDrawerOpen(true)}
                onEditEntry={() => {}}
              />
            ) : (
              <TimetableGrid
                entries={weeklyEntries}
                onAddEntry={handleAddEntry}
                onEditEntry={() => {}}
                onDeleteEntry={handleDeleteEntry}
                className="h-full"
              />
            )}
          </div>
        </div>
      </AppShell>

      {/* Daily Add Entry Drawer */}
      <AddEntryDrawer
        open={dailyDrawerOpen}
        onOpenChange={setDailyDrawerOpen}
        onSubmit={handleDailyAddEntry}
      />
    </>
  )
}
