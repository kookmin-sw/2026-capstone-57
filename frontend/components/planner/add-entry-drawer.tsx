"use client"

import { useState, useEffect } from "react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import {
  Drawer,
  DrawerContent,
  DrawerHeader,
  DrawerTitle,
  DrawerFooter,
} from "@/components/ui/drawer"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import type { EntryType } from "@/types/planner"
import { ENTRY_TYPE_LABELS } from "@/types/planner"

export interface AddEntryFormData {
  courseName: string
  location: string
  startTime: string
  endTime: string
  type: EntryType
}

// 30분 단위 시간 옵션 생성 (09:00 ~ 21:00)
const TIME_OPTIONS = Array.from({ length: 25 }, (_, i) => {
  const hour = Math.floor(i / 2) + 9
  const minute = (i % 2) * 30
  const value = `${hour.toString().padStart(2, "0")}:${minute === 0 ? "00" : "30"}`
  const label = `${hour > 12 ? hour - 12 : hour}:${minute === 0 ? "00" : "30"} ${hour >= 12 ? "PM" : "AM"}`
  return { value, label }
})

interface AddEntryDrawerProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  initialStartTime?: string
  initialEndTime?: string
  onSubmit: (data: AddEntryFormData) => void
}

export function AddEntryDrawer({
  open,
  onOpenChange,
  initialStartTime = "",
  initialEndTime = "",
  onSubmit,
}: AddEntryDrawerProps) {
  const [formData, setFormData] = useState<AddEntryFormData>({
    courseName: "",
    location: "",
    startTime: initialStartTime,
    endTime: initialEndTime,
    type: "CLASS",
  })

  useEffect(() => {
    if (open) {
      setFormData({
        courseName: "",
        location: "",
        startTime: initialStartTime,
        endTime: initialEndTime,
        type: "CLASS",
      })
    }
  }, [open, initialStartTime, initialEndTime])

  const handleSubmit = () => {
    if (!formData.courseName.trim()) return
    onSubmit(formData)
    onOpenChange(false)
  }

  return (
    <Drawer open={open} onOpenChange={onOpenChange}>
      <DrawerContent>
        <div className="mx-auto w-full max-w-md px-4 pb-6">
          <DrawerHeader className="px-0">
            <DrawerTitle>일정 추가</DrawerTitle>
          </DrawerHeader>

          <div className="space-y-5">
            {/* Time selectors */}
            <div className="flex items-center gap-2">
              <div className="flex-1">
                <Label className="text-xs text-muted-foreground">시작</Label>
                <Select
                  value={formData.startTime}
                  onValueChange={(value) =>
                    setFormData((prev) => ({ ...prev, startTime: value }))
                  }
                >
                  <SelectTrigger className="mt-1 h-11 text-base w-full">
                    <SelectValue placeholder="시작 시간" />
                  </SelectTrigger>
                  <SelectContent className="max-h-60">
                    {TIME_OPTIONS.map((opt) => (
                      <SelectItem key={opt.value} value={opt.value}>
                        {opt.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <span className="mt-5 text-muted-foreground">~</span>
              <div className="flex-1">
                <Label className="text-xs text-muted-foreground">종료</Label>
                <Select
                  value={formData.endTime}
                  onValueChange={(value) =>
                    setFormData((prev) => ({ ...prev, endTime: value }))
                  }
                >
                  <SelectTrigger className="mt-1 h-11 text-base w-full">
                    <SelectValue placeholder="종료 시간" />
                  </SelectTrigger>
                  <SelectContent className="max-h-60">
                    {TIME_OPTIONS.map((opt) => (
                      <SelectItem key={opt.value} value={opt.value}>
                        {opt.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Type selector */}
            <div>
              <Label className="text-xs text-muted-foreground">유형</Label>
              <div className="grid grid-cols-3 gap-2 mt-1.5">
                {(["CLASS", "FREE", "ACTIVITY"] as EntryType[]).map((type) => (
                  <button
                    key={type}
                    type="button"
                    onClick={() => setFormData((prev) => ({ ...prev, type }))}
                    className={cn(
                      "py-2.5 px-3 rounded-lg border text-sm font-medium transition-colors",
                      formData.type === type
                        ? "border-primary bg-primary/10 text-primary"
                        : "border-border bg-card text-muted-foreground hover:bg-muted"
                    )}
                  >
                    {ENTRY_TYPE_LABELS[type]}
                  </button>
                ))}
              </div>
            </div>

            {/* Name */}
            <div>
              <Label htmlFor="drawer-courseName" className="text-xs text-muted-foreground">
                이름
              </Label>
              <Input
                id="drawer-courseName"
                placeholder="예: 데이터베이스, 스터디 모임"
                value={formData.courseName}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, courseName: e.target.value }))
                }
                className="mt-1 h-11 text-base"
              />
            </div>

            {/* Location */}
            <div>
              <Label htmlFor="drawer-location" className="text-xs text-muted-foreground">
                장소
              </Label>
              <Input
                id="drawer-location"
                placeholder="예: 공학관 401"
                value={formData.location}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, location: e.target.value }))
                }
                className="mt-1 h-11 text-base"
              />
            </div>
          </div>

          <DrawerFooter className="px-0 pt-6">
            <Button
              onClick={handleSubmit}
              disabled={!formData.courseName.trim()}
              className="w-full h-12 text-base font-medium"
            >
              추가
            </Button>
            <Button
              variant="ghost"
              onClick={() => onOpenChange(false)}
              className="w-full h-11 text-base"
            >
              취소
            </Button>
          </DrawerFooter>
        </div>
      </DrawerContent>
    </Drawer>
  )
}
