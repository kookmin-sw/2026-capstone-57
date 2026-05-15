"use client"

import { useState, useCallback, useRef } from "react"
import { X, Plus } from "lucide-react"
import { cn } from "@/lib/utils"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
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

// 30분 단위 슬롯: 9:00 ~ 20:30 (23개 슬롯)
const HALF_HOUR_SLOTS = Array.from({ length: 23 }, (_, i) => {
  const hour = Math.floor(i / 2) + 9
  const minute = (i % 2) * 30
  return { hour, minute, label: `${hour}:${minute === 0 ? "00" : "30"}` }
})

const DAYS: DayOfWeek[] = [1, 2, 3, 4, 5] // Mon to Fri

function parseTime(time: string): number {
  const [hour, minute] = time.split(":").map(Number)
  return hour + minute / 60
}

function slotToTime(slotIndex: number): string {
  const hour = Math.floor(slotIndex / 2) + 9
  const minute = (slotIndex % 2) * 30
  return `${hour.toString().padStart(2, "0")}:${minute === 0 ? "00" : "30"}`
}

function getEntryStyle(entry: TimetableEntry) {
  const startHour = parseTime(entry.startTime)
  const endHour = parseTime(entry.endTime)
  const top = (startHour - 9) * 48
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
  const [isDragging, setIsDragging] = useState(false)
  const [dragDay, setDragDay] = useState<DayOfWeek | null>(null)
  const [dragStart, setDragStart] = useState<number | null>(null)
  const [dragEnd, setDragEnd] = useState<number | null>(null)

  const [selection, setSelection] = useState<{
    day: DayOfWeek
    startSlot: number
    endSlot: number
  } | null>(null)

  const [dialogOpen, setDialogOpen] = useState(false)
  const [initialStartTime, setInitialStartTime] = useState("")
  const [initialEndTime, setInitialEndTime] = useState("")

  const gridRef = useRef<HTMLDivElement>(null)

  const entriesByDay = DAYS.reduce((acc, day) => {
    acc[day] = entries.filter((e) => e.dayOfWeek === day)
    return acc
  }, {} as Record<DayOfWeek, TimetableEntry[]>)

  const courseColors = new Map<string, string>()
  entries.forEach((entry) => {
    if (!courseColors.has(entry.courseName)) {
      courseColors.set(
        entry.courseName,
        COURSE_COLORS[courseColors.size % COURSE_COLORS.length]
      )
    }
  })

  // --- Drag handlers ---

  const startDrag = useCallback((day: DayOfWeek, slotIndex: number) => {
    setIsDragging(true)
    setDragDay(day)
    setDragStart(slotIndex)
    setDragEnd(slotIndex)
    setSelection(null)
  }, [])

  const moveDrag = useCallback(
    (day: DayOfWeek, slotIndex: number) => {
      if (isDragging && day === dragDay) {
        setDragEnd(slotIndex)
      }
    },
    [isDragging, dragDay]
  )

  const endDrag = useCallback(() => {
    if (isDragging && dragDay !== null && dragStart !== null && dragEnd !== null) {
      const startSlot = Math.min(dragStart, dragEnd)
      const endSlot = Math.max(dragStart, dragEnd)
      setSelection({ day: dragDay, startSlot, endSlot })
    }
    setIsDragging(false)
    setDragDay(null)
    setDragStart(null)
    setDragEnd(null)
  }, [isDragging, dragDay, dragStart, dragEnd])

  const getSlotFromTouch = useCallback(
    (touch: React.Touch, day: DayOfWeek) => {
      if (!gridRef.current) return null
      const dayColumns = gridRef.current.querySelectorAll("[data-day-column]")
      const dayCol = Array.from(dayColumns).find(
        (el) => el.getAttribute("data-day-column") === String(day)
      )
      if (!dayCol) return null
      const rect = dayCol.getBoundingClientRect()
      const y = touch.clientY - rect.top
      return Math.max(0, Math.min(22, Math.floor(y / 24)))
    },
    []
  )

  const handleTouchStart = useCallback(
    (e: React.TouchEvent, day: DayOfWeek, slotIndex: number) => {
      e.preventDefault()
      startDrag(day, slotIndex)
    },
    [startDrag]
  )

  const handleTouchMove = useCallback(
    (e: React.TouchEvent, day: DayOfWeek) => {
      if (!isDragging || dragDay !== day) return
      e.preventDefault()
      const touch = e.touches[0]
      const slotIndex = getSlotFromTouch(touch, day)
      if (slotIndex !== null) setDragEnd(slotIndex)
    },
    [isDragging, dragDay, getSlotFromTouch]
  )

  const handleTouchEnd = useCallback(
    (e: React.TouchEvent) => {
      e.preventDefault()
      endDrag()
    },
    [endDrag]
  )

  const isSlotInDragRange = (day: DayOfWeek, slotIndex: number) => {
    if (isDragging && dragDay === day && dragStart !== null && dragEnd !== null) {
      const min = Math.min(dragStart, dragEnd)
      const max = Math.max(dragStart, dragEnd)
      return slotIndex >= min && slotIndex <= max
    }
    return false
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
  }

  return (
    <>
      <Card className={cn("overflow-hidden h-full flex flex-col", className)}>
        <CardHeader className="pb-2 shrink-0">
          <CardTitle className="text-base">주간 시간표</CardTitle>
          <p className="text-xs text-muted-foreground">
            빈 칸을 드래그하여 시간을 선택하세요
          </p>
        </CardHeader>
        <CardContent className="p-0 flex-1 overflow-hidden flex flex-col">
          <div className="flex-1 overflow-y-auto overflow-x-hidden">
            <div
              className="min-w-[360px] select-none"
              ref={gridRef}
              onMouseUp={endDrag}
              onMouseLeave={() => { if (isDragging) endDrag() }}
            >
              {/* Header */}
              <div className="grid grid-cols-[40px_repeat(5,1fr)] border-b border-border sticky top-0 bg-card z-10">
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

              {/* Grid */}
              <div className="grid grid-cols-[40px_repeat(5,1fr)]">
                {/* Time labels */}
                <div className="relative">
                  {HALF_HOUR_SLOTS.map((slot, index) => (
                    <div
                      key={index}
                      className={cn(
                        "h-6 flex items-start justify-center pt-0.5",
                        index % 2 === 0
                          ? "border-b border-border"
                          : "border-b border-border/30"
                      )}
                    >
                      {index % 2 === 0 && (
                        <span className="text-[9px] text-muted-foreground leading-none">
                          {slot.hour}:{slot.minute === 0 ? "00" : "30"}
                        </span>
                      )}
                    </div>
                  ))}
                </div>

                {/* Day columns */}
                {DAYS.map((day) => (
                  <div
                    key={day}
                    className="relative border-l border-border"
                    data-day-column={day}
                  >
                    {HALF_HOUR_SLOTS.map((_slot, slotIndex) => (
                      <div
                        key={slotIndex}
                        className={cn(
                          "h-6 transition-colors",
                          slotIndex % 2 === 0
                            ? "border-b border-border"
                            : "border-b border-border/30",
                          isSlotInDragRange(day, slotIndex) && "bg-primary/25",
                          isSlotInSelection(day, slotIndex) && !isDragging && "bg-primary/15"
                        )}
                        onMouseDown={(e) => { e.preventDefault(); startDrag(day, slotIndex) }}
                        onMouseEnter={() => moveDrag(day, slotIndex)}
                        onTouchStart={(e) => handleTouchStart(e, day, slotIndex)}
                        onTouchMove={(e) => handleTouchMove(e, day)}
                        onTouchEnd={handleTouchEnd}
                      />
                    ))}

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
                          onClick={(e) => { e.stopPropagation(); onEditEntry?.(entry) }}
                        >
                          <div className="flex items-start justify-between gap-0.5">
                            <div className="min-w-0 flex-1">
                              <p className="text-[9px] font-medium truncate">{entry.courseName}</p>
                              <p className="text-[8px] opacity-75 truncate">{entry.location}</p>
                            </div>
                            {onDeleteEntry && (
                              <button
                                onClick={(e) => { e.stopPropagation(); onDeleteEntry(entry.id) }}
                                className="p-0.5 rounded hover:bg-black/10 shrink-0"
                              >
                                <X className="w-2.5 h-2.5" />
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

          {/* Selection action bar */}
          {selection && !isDragging && (
            <div className="shrink-0 border-t border-border bg-card p-3 flex items-center justify-between gap-2">
              <div className="text-sm text-muted-foreground">
                <span className="font-medium text-foreground">
                  {DAY_LABELS[selection.day]}
                </span>{" "}
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
    </>
  )
}
