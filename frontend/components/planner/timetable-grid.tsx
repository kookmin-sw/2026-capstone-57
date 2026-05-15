"use client"

import { useState } from "react"
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

const HALF_HOUR_SLOTS = Array.from({ length: 23 }, (_, i) => {
  const hour = Math.floor(i / 2) + 9
  const minute = (i % 2) * 30
  return { hour, minute, label: `${hour}:${minute === 0 ? "00" : "30"}` }
})

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
  // Click-based selection: first click sets start, second click sets end (same day)
  const [firstClick, setFirstClick] = useState<{ day: DayOfWeek; slot: number } | null>(null)
  const [selection, setSelection] = useState<{
    day: DayOfWeek
    startSlot: number
    endSlot: number
  } | null>(null)

  const [dialogOpen, setDialogOpen] = useState(false)
  const [initialStartTime, setInitialStartTime] = useState("")
  const [initialEndTime, setInitialEndTime] = useState("")

  // Edit mode
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

  // --- Click handler ---
  const handleSlotClick = (day: DayOfWeek, slotIndex: number) => {
    if (!firstClick) {
      // First click: set start point
      setFirstClick({ day, slot: slotIndex })
      setSelection(null)
    } else if (firstClick.day !== day) {
      // Different day: reset, this becomes new first click
      setFirstClick({ day, slot: slotIndex })
      setSelection(null)
    } else {
      // Same day, second click: create range
      const startSlot = Math.min(firstClick.slot, slotIndex)
      const endSlot = Math.max(firstClick.slot, slotIndex)
      setSelection({ day, startSlot, endSlot })
      setFirstClick(null)
    }
  }

  // --- Highlight helpers ---
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
      <Card className={cn("overflow-hidden h-full flex flex-col", className)}>
        <CardHeader className="pb-2 shrink-0">
          <CardTitle className="text-base">주간 시간표</CardTitle>
          <p className="text-xs text-muted-foreground">
            시작 시간과 종료 시간을 탭하여 선택하세요
          </p>
        </CardHeader>
        <CardContent className="p-0 flex-1 overflow-hidden flex flex-col">
          <div className="flex-1 overflow-y-auto overflow-x-hidden">
            <div className="min-w-[360px] select-none">
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
                {/* Time labels - positioned at hour boundary lines */}
                <div className="relative pt-0.5">
                  {HALF_HOUR_SLOTS.map((slot, index) => (
                    <div
                      key={index}
                      className="h-6 relative"
                    >
                      {/* Show hour label at top of even slots (9:00, 10:00...) */}
                      {index % 2 === 0 && (
                        <span className="absolute top-[-1px] right-1.5 text-[10px] font-medium text-foreground leading-none">
                          {slot.hour}:00
                        </span>
                      )}
                    </div>
                  ))}
                </div>

                {/* Day columns */}
                {DAYS.map((day) => (
                  <div key={day} className="relative border-l border-border" data-day-column={day}>
                    {HALF_HOUR_SLOTS.map((_slot, slotIndex) => (
                      <div
                        key={slotIndex}
                        className={cn(
                          "h-6 transition-colors cursor-pointer",
                          slotIndex % 2 === 1 ? "border-b border-border" : "border-b border-dashed border-border/80",
                          isSlotFirstClick(day, slotIndex) && "bg-primary/30",
                          isSlotInSelection(day, slotIndex) && "bg-primary/15"
                        )}
                        onClick={() => handleSlotClick(day, slotIndex)}
                      />
                    ))}

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

      {/* Edit Entry Drawer */}
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
