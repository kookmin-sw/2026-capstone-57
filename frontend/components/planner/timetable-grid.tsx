"use client"

import { useState } from "react"
import { X, Plus } from "lucide-react"
import { cn } from "@/lib/utils"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { AddEntryDrawer, type AddEntryFormData } from "./add-entry-drawer"
import type { TimetableEntry, DayOfWeek } from "@/types/planner"
import { DAY_LABELS, COURSE_COLORS } from "@/types/planner"

interface TimetableGridProps {
  entries: TimetableEntry[]
  onAddEntry?: (entry: Omit<TimetableEntry, "id" | "userId">) => void
  onEditEntry?: (entry: TimetableEntry) => void
  onDeleteEntry?: (entryId: string) => void
  className?: string
}

const HALF_HOUR_SLOTS = Array.from({ length: 23 }, (_, i) => {
  const hour = Math.floor(i / 2) + 9
  const minute = (i % 2) * 30
  return { hour, minute, label: `${hour}:${minute === 0 ? "00" : "30"}` }
})

const TOTAL_HOURS = HALF_HOUR_SLOTS.length * 0.5 // 11.5 hours (9:00 ~ 20:30)

const DAYS: DayOfWeek[] = [1, 2, 3, 4, 5]

function parseTime(time: string): number {
  const [hour, minute] = time.split(":").map(Number)
  return hour + minute / 60
}

function slotToTime(slotIndex: number): string {
  const hour = Math.floor(slotIndex / 2) + 9
  const minute = (slotIndex % 2) * 30
  return `${hour.toString().padStart(2, "0")}:${minute === 0 ? "00" : "30"}`
}

// % 기반으로 변경 - 동적 높이에도 정확히 배치
function getEntryStyle(entry: TimetableEntry) {
  const startHour = parseTime(entry.startTime)
  const endHour = parseTime(entry.endTime)
  const topPct = ((startHour - 9) / TOTAL_HOURS) * 100
  const heightPct = ((endHour - startHour) / TOTAL_HOURS) * 100
  return { top: `${topPct}%`, height: `${heightPct}%` }
}

export function TimetableGrid({
  entries,
  onAddEntry,
  onEditEntry,
  onDeleteEntry,
  className,
}: TimetableGridProps) {
  const [firstClick, setFirstClick] = useState<{ day: DayOfWeek; slot: number } | null>(null)
  const [selection, setSelection] = useState<{
    day: DayOfWeek
    startSlot: number
    endSlot: number
  } | null>(null)

  const [dialogOpen, setDialogOpen] = useState(false)
  const [initialStartTime, setInitialStartTime] = useState("")
  const [initialEndTime, setInitialEndTime] = useState("")

  const [editDialogOpen, setEditDialogOpen] = useState(false)
  const [editingEntry, setEditingEntry] = useState<TimetableEntry | null>(null)

  const entriesByDay = DAYS.reduce((acc, day) => {
    acc[day] = entries.filter((e) => e.dayOfWeek === day)
    return acc
  }, {} as Record<DayOfWeek, TimetableEntry[]>)

  const courseColors = new Map<string, string>()
  entries.forEach((entry) => {
    if (!courseColors.has(entry.courseName)) {
      courseColors.set(entry.courseName, COURSE_COLORS[courseColors.size % COURSE_COLORS.length])
    }
  })

  const handleSlotClick = (day: DayOfWeek, slotIndex: number) => {
    if (!firstClick) {
      setFirstClick({ day, slot: slotIndex })
      setSelection(null)
    } else if (firstClick.day !== day) {
      setFirstClick({ day, slot: slotIndex })
      setSelection(null)
    } else {
      const startSlot = Math.min(firstClick.slot, slotIndex)
      const endSlot = Math.max(firstClick.slot, slotIndex)
      setSelection({ day, startSlot, endSlot })
      setFirstClick(null)
    }
  }

  const isSlotFirstClick = (day: DayOfWeek, slotIndex: number) => {
    return firstClick?.day === day && firstClick?.slot === slotIndex
  }

  const isSlotInSelection = (day: DayOfWeek, slotIndex: number) => {
    if (!selection) return false
    return (
      selection.day === day &&
      slotIndex >= selection.startSlot &&
      slotIndex <= selection.endSlot
    )
  }

  const openAddDialog = () => {
    if (!selection) return
    setInitialStartTime(slotToTime(selection.startSlot))
    setInitialEndTime(slotToTime(selection.endSlot + 1))
    setDialogOpen(true)
  }

  const handleSubmit = (data: AddEntryFormData) => {
    if (!selection) return
    onAddEntry?.({
      dayOfWeek: selection.day,
      startTime: data.startTime,
      endTime: data.endTime,
      courseName: data.courseName,
      location: data.location,
      type: data.type,
    })
    setSelection(null)
  }

  const cancelSelection = () => {
    setSelection(null)
    setFirstClick(null)
  }

  return (
    <>
      {/* ✅ Card가 부모 높이를 꽉 채우도록 h-full 유지 */}
      <Card className={cn("overflow-hidden flex flex-col h-full", className)}>
        <CardContent className="p-0 flex-1 overflow-hidden flex flex-col">

          {/* ✅ 스크롤 제거, flex-col로 전체 높이 사용 */}
          <div className="flex-1 overflow-hidden flex flex-col min-w-[360px] select-none">

            {/* Header - sticky */}
            <div className="grid grid-cols-[40px_repeat(5,1fr)] border-b border-border bg-card z-10 shrink-0">
              <div className="p-1 text-center text-xs text-muted-foreground" />
              {DAYS.map((day) => (
                <div
                  key={day}
                  className="p-1.5 text-center text-xs font-medium text-foreground border-l border-border"
                >
                  {DAY_LABELS[day]}
                </div>
              ))}
            </div>

            {/* ✅ Grid 영역 - flex-1로 남은 공간 전부 사용 */}
            <div className="grid grid-cols-[40px_repeat(5,1fr)] flex-1">

              {/* ✅ 시간 레이블 컬럼 - flex-col로 슬롯 균등 배분 */}
              <div className="relative flex flex-col pt-0.5">
                {HALF_HOUR_SLOTS.map((slot, index) => (
                  <div key={index} className="flex-1 relative">
                    {index % 2 === 0 && (
                      <span className="absolute top-[-1px] right-1.5 text-[10px] font-medium text-foreground leading-none">
                        {slot.hour}:00
                      </span>
                    )}
                  </div>
                ))}
              </div>

              {/* ✅ 요일 컬럼 - flex-col로 슬롯 균등 배분 */}
              {DAYS.map((day) => (
                <div key={day} className="relative border-l border-border flex flex-col" data-day-column={day}>
                  {HALF_HOUR_SLOTS.map((_slot, slotIndex) => (
                    <div
                      key={slotIndex}
                      className={cn(
                        // ✅ h-6 → flex-1 로 변경
                        "flex-1 transition-colors cursor-pointer",
                        slotIndex % 2 === 1
                          ? "border-b border-border"
                          : "border-b border-dashed border-border/80",
                        isSlotFirstClick(day, slotIndex) && "bg-primary/30",
                        isSlotInSelection(day, slotIndex) && "bg-primary/15"
                      )}
                      onClick={() => handleSlotClick(day, slotIndex)}
                    />
                  ))}

                  {/* 수업 엔트리 - % 기반 위치로 동적 높이에 대응 */}
                  {entriesByDay[day]?.map((entry) => {
                    const style = getEntryStyle(entry)
                    const colorClass = courseColors.get(entry.courseName) || COURSE_COLORS[0]
                    return (
                      <div
                        key={entry.id}
                        className={cn(
                          "absolute left-0.5 right-0.5 rounded-md border p-1.5 cursor-pointer",
                          "overflow-hidden transition-shadow hover:shadow-md",
                          colorClass
                        )}
                        style={style}
                        onClick={(e) => {
                          e.stopPropagation()
                          setEditingEntry(entry)
                          setEditDialogOpen(true)
                        }}
                      >
                        <p className="text-[11px] font-semibold leading-tight break-words">
                          {entry.courseName}
                        </p>
                        <p className="text-[10px] opacity-70 leading-tight mt-0.5 break-words">
                          {entry.location}
                        </p>
                      </div>
                    )
                  })}
                </div>
              ))}
            </div>
          </div>

          {/* First click indicator */}
          {firstClick && !selection && (
            <div className="shrink-0 border-t border-border bg-card p-3 flex items-center justify-between gap-2">
              <div className="text-sm text-muted-foreground">
                <span className="font-medium text-foreground">{DAY_LABELS[firstClick.day]}</span>{" "}
                {slotToTime(firstClick.slot)} 선택됨 — 종료 시간을 탭하세요
              </div>
              <Button variant="ghost" size="sm" onClick={cancelSelection} className="text-xs">
                취소
              </Button>
            </div>
          )}

          {/* Selection action bar */}
          {selection && (
            <div className="shrink-0 border-t border-border bg-card p-3 flex items-center justify-between gap-2">
              <div className="text-sm text-muted-foreground">
                <span className="font-medium text-foreground">{DAY_LABELS[selection.day]}</span>{" "}
                {slotToTime(selection.startSlot)} ~ {slotToTime(selection.endSlot + 1)}
              </div>
              <div className="flex gap-2">
                <Button variant="ghost" size="sm" onClick={cancelSelection} className="text-xs">
                  취소
                </Button>
                <Button size="sm" onClick={openAddDialog} className="text-xs gap-1">
                  <Plus className="w-3.5 h-3.5" />
                  추가하기
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <AddEntryDrawer
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        initialStartTime={initialStartTime}
        initialEndTime={initialEndTime}
        onSubmit={handleSubmit}
      />

      <AddEntryDrawer
        open={editDialogOpen}
        onOpenChange={setEditDialogOpen}
        editMode
        initialData={editingEntry ? {
          courseName: editingEntry.courseName,
          location: editingEntry.location,
          startTime: editingEntry.startTime,
          endTime: editingEntry.endTime,
          type: editingEntry.type,
        } : null}
        onSubmit={(data) => {
          if (editingEntry) {
            onEditEntry?.({ ...editingEntry, ...data, courseName: data.courseName, location: data.location, startTime: data.startTime, endTime: data.endTime, type: data.type })
          }
          setEditingEntry(null)
        }}
        onDelete={() => {
          if (editingEntry) {
            onDeleteEntry?.(editingEntry.id)
          }
          setEditingEntry(null)
        }}
      />
    </>
  )
}