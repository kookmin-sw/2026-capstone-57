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
    icon: <Clock className="w-4 h-4 text-muted-foreground" />,
    label: "대기 중",
  },
  ACTIVE: {
    bgColor: "bg-gradient-to-br from-primary/5 to-primary/10",
    borderColor: "border-solid border-primary/30",
    icon: <Sparkles className="w-4 h-4 text-primary" />,
    label: "진행 중",
  },
  COMPLETED: {
    bgColor: "bg-secondary/20",
    borderColor: "border-solid border-secondary/50",
    icon: <Check className="w-4 h-4 text-foreground" />,
    label: "완료",
  },
}

const priorityConfig: Record<SlotPriority, { icon: React.ReactNode; color: string }> = {
  HOBBY: { icon: <Star className="w-3.5 h-3.5" />, color: "text-accent" },
  INTEREST: { icon: <Sparkles className="w-3.5 h-3.5" />, color: "text-primary" },
  IDEAL_TYPE: { icon: <Heart className="w-3.5 h-3.5" />, color: "text-destructive" },
}

const stageIcons: Record<InteractionStage, React.ReactNode> = {
  QUIZ: <BookOpen className="w-4 h-4" />,
  CHAT: <MessageCircle className="w-4 h-4" />,
  GAME: <Gamepad2 className="w-4 h-4" />,
  MISSION: <MapPin className="w-4 h-4" />,
  REVIEW: <BookOpen className="w-4 h-4" />,
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
      }, 1000 * 60) // Update every minute
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
          "border-2",
          slot.status === "ACTIVE" && "shadow-md",
          className
        )}
        onClick={() => onSlotClick?.(slot)}
      >
        <CardContent className="p-4">
          {/* Header: Slot Number, Status Badge & Quick Match */}
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <span className="text-xs font-medium text-muted-foreground">
                슬롯 {slot.slotNumber}
              </span>
              <div className={cn(
                "flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium",
                slot.status === "EMPTY" && "bg-muted text-muted-foreground",
                slot.status === "ACTIVE" && "bg-primary/15 text-primary",
                slot.status === "COMPLETED" && "bg-secondary text-foreground"
              )}>
                {config.icon}
                <span>{config.label}</span>
              </div>
            </div>
            {slot.isQuickMatch && (
              <div className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-accent text-accent-foreground text-xs font-medium">
                <Zap className="w-3 h-3" />
                <span>빠른매칭</span>
              </div>
            )}
          </div>

          {/* EMPTY State Content */}
          {slot.status === "EMPTY" && (
            <div className="py-4">
              <div className="w-14 h-14 rounded-full bg-muted/80 flex items-center justify-center mx-auto mb-3 border-2 border-dashed border-muted-foreground/20">
                <Clock className="w-7 h-7 text-muted-foreground/60" />
              </div>
              <p className="text-center text-sm text-muted-foreground mb-1">
                새로운 인연을 기다리고 있어요
              </p>
              <p className="text-center text-xs text-muted-foreground/80 mb-3">
                다음 월요일에 매칭이 시작됩니다
              </p>
              {/* Countdown */}
              <div className="flex items-center justify-center gap-3 text-center">
                <div className="px-3 py-1.5 rounded-lg bg-muted">
                  <span className="text-lg font-bold text-foreground">{countdown.days}</span>
                  <span className="text-xs text-muted-foreground ml-1">일</span>
                </div>
                <div className="px-3 py-1.5 rounded-lg bg-muted">
                  <span className="text-lg font-bold text-foreground">{countdown.hours}</span>
                  <span className="text-xs text-muted-foreground ml-1">시간</span>
                </div>
              </div>
            </div>
          )}

          {/* ACTIVE State Content */}
          {slot.status === "ACTIVE" && slot.matchedUser && (
            <div className="py-2">
              {/* Matched User */}
              <div className="flex items-center gap-3 mb-4">
                <div className="w-12 h-12 rounded-full bg-primary/20 flex items-center justify-center ring-2 ring-primary/30">
                  <span className="text-xl">
                    {slot.matchedUser.profileEmoji || slot.matchedUser.nickname.charAt(0)}
                  </span>
                </div>
                <div className="flex-1">
                  <p className="font-semibold text-foreground">
                    {slot.matchedUser.nickname}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {slot.daysRemaining !== undefined && `${slot.daysRemaining}일 남음`}
                  </p>
                </div>
                <ChevronRight className="w-5 h-5 text-muted-foreground" />
              </div>

              {/* Current Stage */}
              {slot.currentStage && (
                <div className="mb-3">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-1.5 text-primary">
                      {stageIcons[slot.currentStage]}
                      <span className="text-sm font-medium">
                        {STAGE_LABELS[slot.currentStage]} 단계
                      </span>
                    </div>
                    <span className="text-xs text-muted-foreground">
                      {currentStageIndex + 1} / {STAGE_ORDER.length}
                    </span>
                  </div>
                  
                  {/* Stage Progress Bar */}
                  <div className="flex gap-1">
                    {STAGE_ORDER.map((stage, index) => (
                      <div 
                        key={stage}
                        className={cn(
                          "h-1.5 flex-1 rounded-full transition-colors",
                          index < currentStageIndex && "bg-primary",
                          index === currentStageIndex && "bg-primary/60",
                          index > currentStageIndex && "bg-muted"
                        )}
                      />
                    ))}
                  </div>
                </div>
              )}

              <Button 
                variant="default" 
                size="sm" 
                className="w-full bg-primary hover:bg-primary/90"
              >
                {slot.currentStage === "QUIZ" && "퀴즈 풀기"}
                {slot.currentStage === "CHAT" && "대화하기"}
                {slot.currentStage === "GAME" && "게임하기"}
                {slot.currentStage === "MISSION" && "미션 확인"}
                {slot.currentStage === "REVIEW" && "회고 작성"}
                {!slot.currentStage && "시작하기"}
              </Button>
            </div>
          )}

          {/* COMPLETED State Content */}
          {slot.status === "COMPLETED" && (
            <div className="py-4">
              <div className="w-14 h-14 rounded-full bg-secondary/50 flex items-center justify-center mx-auto mb-3">
                <Check className="w-7 h-7 text-foreground" />
              </div>
              <p className="text-center text-sm font-medium text-foreground mb-1">
                이번 매칭 완료
              </p>
              <p className="text-center text-xs text-muted-foreground">
                다음 주에 새로운 인연을 만나보세요
              </p>
              {slot.matchedUser && (
                <div className="mt-3 p-2 rounded-lg bg-muted/50 text-center">
                  <span className="text-xs text-muted-foreground">
                    {slot.matchedUser.nickname}님과의 만남이 끝났어요
                  </span>
                </div>
              )}
            </div>
          )}

          {/* Priority Selector */}
          <div className="mt-3 pt-3 border-t border-border/30">
            <button
              onClick={(e) => {
                e.stopPropagation()
                setShowPrioritySelector(true)
              }}
              className="flex items-center justify-between w-full text-sm hover:bg-muted/50 rounded-lg p-1.5 -m-1.5 transition-colors"
            >
              <span className="text-muted-foreground">매칭 우선순위</span>
              <div className={cn("flex items-center gap-1.5 font-medium", priorityStyle.color)}>
                {priorityStyle.icon}
                <span>{PRIORITY_LABELS[slot.priority]}</span>
              </div>
            </button>
          </div>
        </CardContent>
      </Card>

      {/* Priority Selector Bottom Sheet */}
      <SlotPrioritySelector
        isOpen={showPrioritySelector}
        currentPriority={slot.priority}
        onSelect={handlePriorityChange}
        onClose={() => setShowPrioritySelector(false)}
      />
    </>
  )
}

// Locked Slot Card for slots that need to be unlocked
interface LockedSlotCardProps {
  lockedSlot: LockedSlot
  onUnlock?: () => void
  className?: string
}

export function LockedSlotCard({ lockedSlot, onUnlock, className }: LockedSlotCardProps) {
  const progressPercent = Math.min((lockedSlot.currentLevel / lockedSlot.requiredLevel) * 100, 100)

  return (
    <Card className={cn(
      "relative overflow-hidden border-2 border-dashed border-muted-foreground/20 bg-muted/20",
      className
    )}>
      <CardContent className="p-4">
        <div className="flex items-center justify-between mb-3">
          <span className="text-xs font-medium text-muted-foreground">
            슬롯 {lockedSlot.slotNumber}
          </span>
          <div className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-muted text-muted-foreground text-xs font-medium">
            <Lock className="w-3 h-3" />
            <span>잠김</span>
          </div>
        </div>

        <div className="py-4">
          <div className="w-14 h-14 rounded-full bg-muted/60 flex items-center justify-center mx-auto mb-3">
            <Lock className="w-7 h-7 text-muted-foreground/60" />
          </div>
          <p className="text-center text-sm font-medium text-foreground mb-1">
            새로운 슬롯
          </p>
          <p className="text-center text-xs text-muted-foreground mb-4">
            레벨 {lockedSlot.requiredLevel} 달성 시 해금
          </p>

          {/* Level Progress */}
          <div className="mb-4">
            <div className="flex items-center justify-between text-xs mb-1">
              <span className="text-muted-foreground">현재 레벨</span>
              <span className="text-foreground font-medium">
                Lv.{lockedSlot.currentLevel} / Lv.{lockedSlot.requiredLevel}
              </span>
            </div>
            <div className="h-2 bg-muted rounded-full overflow-hidden">
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
            className="w-full border-primary/50 text-primary hover:bg-primary/10 disabled:opacity-50"
          >
            {lockedSlot.currentLevel >= lockedSlot.requiredLevel ? "해금하기" : "레벨업 필요"}
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
