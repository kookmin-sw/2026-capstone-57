"use client"

import { useState, useEffect } from "react"
import {
  Clock, Heart, Sparkles, Star, Check, Lock, Zap,
  MessageCircle, Gamepad2, MapPin, BookOpen, ChevronRight
} from "lucide-react"
import { cn } from "@/lib/utils"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { SlotPrioritySelector } from "./slot-priority-selector"
import type { Slot, SlotPriority, SlotStatus, InteractionStage, LockedSlot } from "@/types/slot"
import { PRIORITY_LABELS, STAGE_LABELS, STAGE_ORDER } from "@/types/slot"

interface SlotCardProps {
  slot: Slot
  onPriorityChange?: (slotId: string, priority: SlotPriority) => void
  onSlotClick?: (slot: Slot) => void
  className?: string
}

const statusConfig: Record<SlotStatus, {
  bgColor: string
  borderColor: string
  icon: React.ReactNode
  label: string
}> = {
  EMPTY: {
    bgColor: "bg-muted/50",
    borderColor: "border-dashed border-muted-foreground/30",
    icon: <Clock className="w-3 h-3 text-muted-foreground" />,
    label: "대기 중",
  },
  ACTIVE: {
    bgColor: "bg-gradient-to-br from-primary/5 to-primary/10",
    borderColor: "border-solid border-primary/30",
    icon: <Sparkles className="w-3 h-3 text-primary" />,
    label: "진행 중",
  },
  COMPLETED: {
    bgColor: "bg-secondary/20",
    borderColor: "border-solid border-secondary/50",
    icon: <Check className="w-3 h-3 text-foreground" />,
    label: "완료",
  },
}

const priorityConfig: Record<SlotPriority, { icon: React.ReactNode; color: string }> = {
  HOBBY: { icon: <Star className="w-3 h-3" />, color: "text-accent" },
  INTEREST: { icon: <Sparkles className="w-3 h-3" />, color: "text-primary" },
  IDEAL_TYPE: { icon: <Heart className="w-3 h-3" />, color: "text-destructive" },
}

const stageIcons: Record<InteractionStage, React.ReactNode> = {
  QUIZ: <BookOpen className="w-3 h-3" />,
  CHAT: <MessageCircle className="w-3 h-3" />,
  GAME: <Gamepad2 className="w-3 h-3" />,
  MISSION: <MapPin className="w-3 h-3" />,
  REVIEW: <BookOpen className="w-3 h-3" />,
}

function getCountdownToMonday(): { days: number; hours: number } {
  const now = new Date()
  const dayOfWeek = now.getDay()
  const daysUntilMonday = dayOfWeek === 0 ? 1 : (8 - dayOfWeek)

  const nextMonday = new Date(now)
  nextMonday.setDate(now.getDate() + daysUntilMonday)
  nextMonday.setHours(0, 0, 0, 0)

  const diff = nextMonday.getTime() - now.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))

  return { days, hours }
}

export function SlotCard({ slot, onPriorityChange, onSlotClick, className }: SlotCardProps) {
  const [showPrioritySelector, setShowPrioritySelector] = useState(false)
  const [countdown, setCountdown] = useState({ days: 0, hours: 0 })
  const config = statusConfig[slot.status]
  const priorityStyle = priorityConfig[slot.priority]

  useEffect(() => {
    if (slot.status === "EMPTY") {
      setCountdown(getCountdownToMonday())
      const interval = setInterval(() => {
        setCountdown(getCountdownToMonday())
      }, 1000 * 60)
      return () => clearInterval(interval)
    }
  }, [slot.status])

  const handlePriorityChange = (priority: SlotPriority) => {
    onPriorityChange?.(slot.id, priority)
    setShowPrioritySelector(false)
  }

  const currentStageIndex = slot.currentStage
    ? STAGE_ORDER.indexOf(slot.currentStage)
    : 0

  return (
    <>
      <Card
        className={cn(
          "relative overflow-hidden transition-all duration-200",
          config.bgColor,
          config.borderColor,
          "border",
          slot.status === "ACTIVE" && "shadow-sm",
          className
        )}
        onClick={() => onSlotClick?.(slot)}
      >
        <CardContent className="px-3 py-2">
          {/* Header row */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-1.5">
              <span className="text-[11px] font-medium text-muted-foreground">
                슬롯 {slot.slotNumber}
              </span>
              <div className={cn(
                "flex items-center gap-0.5 px-1.5 py-0.5 rounded-full text-[10px] font-medium",
                slot.status === "EMPTY" && "bg-muted text-muted-foreground",
                slot.status === "ACTIVE" && "bg-primary/15 text-primary",
                slot.status === "COMPLETED" && "bg-secondary text-foreground"
              )}>
                {config.icon}
                <span>{config.label}</span>
              </div>
              {slot.isQuickMatch && (
                <div className="flex items-center gap-0.5 px-1.5 py-0.5 rounded-full bg-accent text-accent-foreground text-[10px] font-medium">
                  <Zap className="w-2.5 h-2.5" />
                  <span>빠른</span>
                </div>
              )}
            </div>
            {/* Priority inline */}
            <button
              onClick={(e) => {
                e.stopPropagation()
                setShowPrioritySelector(true)
              }}
              className={cn("flex items-center gap-0.5 text-[11px] font-medium", priorityStyle.color)}
            >
              {priorityStyle.icon}
              <span>{PRIORITY_LABELS[slot.priority]}</span>
            </button>
          </div>

          {/* EMPTY State - compact */}
          {slot.status === "EMPTY" && (
            <div className="flex items-center justify-between mt-2">
              <p className="text-[11px] text-muted-foreground">
                다음 월요일 매칭 시작
              </p>
              <div className="flex items-center gap-1.5">
                <span className="text-xs font-bold text-foreground">{countdown.days}일</span>
                <span className="text-xs font-bold text-foreground">{countdown.hours}시간</span>
                <span className="text-[10px] text-muted-foreground">남음</span>
              </div>
            </div>
          )}

          {/* ACTIVE State - compact horizontal layout */}
          {slot.status === "ACTIVE" && slot.matchedUser && (
            <div className="mt-2">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center ring-1 ring-primary/30 shrink-0">
                  <span className="text-sm">
                    {slot.matchedUser.profileEmoji || slot.matchedUser.nickname.charAt(0)}
                  </span>
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <p className="font-medium text-xs text-foreground truncate">
                      {slot.matchedUser.nickname}
                    </p>
                    {slot.daysRemaining !== undefined && (
                      <span className="text-[10px] text-muted-foreground shrink-0 ml-1">
                        {slot.daysRemaining}일 남음
                      </span>
                    )}
                  </div>
                  {/* Stage progress inline */}
                  {slot.currentStage && (
                    <div className="flex items-center gap-1.5 mt-1">
                      <div className="flex items-center gap-0.5 text-primary">
                        {stageIcons[slot.currentStage]}
                        <span className="text-[10px] font-medium">
                          {STAGE_LABELS[slot.currentStage]}
                        </span>
                      </div>
                      <div className="flex gap-0.5 flex-1">
                        {STAGE_ORDER.map((stage, index) => (
                          <div
                            key={stage}
                            className={cn(
                              "h-1 flex-1 rounded-full",
                              index < currentStageIndex && "bg-primary",
                              index === currentStageIndex && "bg-primary/60",
                              index > currentStageIndex && "bg-muted"
                            )}
                          />
                        ))}
                      </div>
                      <span className="text-[10px] text-muted-foreground">
                        {currentStageIndex + 1}/{STAGE_ORDER.length}
                      </span>
                    </div>
                  )}
                </div>
                <ChevronRight className="w-4 h-4 text-muted-foreground shrink-0" />
              </div>
            </div>
          )}

          {/* COMPLETED State - compact */}
          {slot.status === "COMPLETED" && (
            <div className="flex items-center gap-2 mt-2">
              <div className="w-8 h-8 rounded-full bg-secondary/50 flex items-center justify-center shrink-0">
                <Check className="w-4 h-4 text-foreground" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs font-medium text-foreground">이번 매칭 완료</p>
                {slot.matchedUser && (
                  <p className="text-[10px] text-muted-foreground truncate">
                    {slot.matchedUser.nickname}님과의 만남이 끝났어요
                  </p>
                )}
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <SlotPrioritySelector
        isOpen={showPrioritySelector}
        currentPriority={slot.priority}
        onSelect={handlePriorityChange}
        onClose={() => setShowPrioritySelector(false)}
      />
    </>
  )
}

// Locked Slot Card
interface LockedSlotCardProps {
  lockedSlot: LockedSlot
  onUnlock?: () => void
  className?: string
}

export function LockedSlotCard({ lockedSlot, onUnlock, className }: LockedSlotCardProps) {
  const progressPercent = Math.min((lockedSlot.currentLevel / lockedSlot.requiredLevel) * 100, 100)

  return (
    <Card className={cn(
      "relative overflow-hidden border border-dashed border-muted-foreground/20 bg-muted/20",
      className
    )}>
      <CardContent className="px-3 py-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5">
            <span className="text-[11px] font-medium text-muted-foreground">
              슬롯 {lockedSlot.slotNumber}
            </span>
            <div className="flex items-center gap-0.5 px-1.5 py-0.5 rounded-full bg-muted text-muted-foreground text-[10px] font-medium">
              <Lock className="w-2.5 h-2.5" />
              <span>잠김</span>
            </div>
          </div>
          <span className="text-[11px] text-muted-foreground">
            Lv.{lockedSlot.currentLevel}/{lockedSlot.requiredLevel}
          </span>
        </div>

        <div className="flex items-center gap-2 mt-2">
          <div className="flex-1">
            <div className="h-1.5 bg-muted rounded-full overflow-hidden">
              <div
                className="h-full bg-primary/60 rounded-full transition-all"
                style={{ width: `${progressPercent}%` }}
              />
            </div>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={onUnlock}
            disabled={lockedSlot.currentLevel < lockedSlot.requiredLevel}
            className="h-6 px-2 text-[10px] border-primary/50 text-primary hover:bg-primary/10 disabled:opacity-50"
          >
            {lockedSlot.currentLevel >= lockedSlot.requiredLevel ? "해금" : "레벨업 필요"}
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
