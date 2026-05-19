"use client"

import { MapPin, Clock, Timer, Calendar, CheckCircle2, XCircle } from "lucide-react"
import { Button } from "@/components/ui/button"
import type { MissionInfo } from "@/types/match"

const DAY_OF_WEEK_KR: Record<string, string> = {
  MONDAY: "월요일",
  TUESDAY: "화요일",
  WEDNESDAY: "수요일",
  THURSDAY: "목요일",
  FRIDAY: "금요일",
  SATURDAY: "토요일",
  SUNDAY: "일요일",
}

interface MissionStageProps {
  mission: MissionInfo
  isWaiting: boolean
  onExtend: () => void
  onComplete: () => void
}

export function MissionStage({ mission, isWaiting, onExtend, onComplete }: MissionStageProps) {
  // 대기 상태 UI
  if (isWaiting) {
    return (
      <div className="bg-card rounded-2xl p-6 shadow-sm border border-border/30 flex flex-col items-center justify-center text-center gap-4 min-h-[260px]">
        <div className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-primary animate-bounce [animation-delay:0ms]" />
          <span className="w-2 h-2 rounded-full bg-primary animate-bounce [animation-delay:150ms]" />
          <span className="w-2 h-2 rounded-full bg-primary animate-bounce [animation-delay:300ms]" />
        </div>
        <div>
          <p className="text-sm font-semibold text-foreground">
            상대방이 미션 완료를 확인하고 있어요
          </p>
          <p className="text-xs text-muted-foreground mt-1">
            잠시만 기다려주세요!
          </p>
        </div>
      </div>
    )
  }

  // 완료 상태 UI
  if (mission.status === "CONFIRMED") {
    return (
      <div className="bg-card rounded-2xl p-6 shadow-sm border border-border/30 flex flex-col items-center justify-center text-center gap-3 min-h-[260px]">
        <CheckCircle2 className="w-10 h-10 text-emerald-500" />
        <p className="text-sm font-semibold text-foreground">미션 완료!</p>
        <p className="text-xs text-muted-foreground">두 분 모두 미션을 확인했어요</p>
      </div>
    )
  }

  // 만료 상태 UI
  if (mission.status === "EXPIRED") {
    return (
      <div className="bg-card rounded-2xl p-6 shadow-sm border border-border/30 flex flex-col items-center justify-center text-center gap-3 min-h-[260px]">
        <XCircle className="w-10 h-10 text-muted-foreground" />
        <p className="text-sm font-semibold text-foreground">미션 기한이 만료되었어요</p>
        <p className="text-xs text-muted-foreground">다음 기회에 만나보세요</p>
      </div>
    )
  }

  // 요일 + 시간대 텍스트 조합
  const dayLabel = mission.dayOfWeek ? DAY_OF_WEEK_KR[mission.dayOfWeek] || mission.dayOfWeek : null
  const scheduleText = [dayLabel, mission.timeSlot].filter(Boolean).join(" · ")

  // PENDING 상태 — 미션 카드
  return (
    <div className="bg-card rounded-2xl p-4 shadow-sm border border-border/30 flex flex-col">
      {/* Header */}
      <div className="shrink-0">
        <h3 className="text-sm font-semibold text-foreground">4단계 · 오프라인 미션</h3>
      </div>

      {/* Info panel */}
      <div className="flex flex-col gap-2.5 my-3">
        {/* Location */}
        <div className="flex items-center gap-2.5">
          <MapPin className="w-4 h-4 text-primary shrink-0" />
          <span className="text-sm font-medium text-foreground">{mission.location}</span>
        </div>

        {/* Activity */}
        <div className="flex items-center gap-2.5">
          <Clock className="w-4 h-4 text-primary shrink-0" />
          <span className="text-sm text-foreground">{mission.activity}</span>
        </div>

        {/* Description */}
        {mission.description && (
          <p className="text-[11px] text-muted-foreground leading-relaxed ml-[26px]">
            {mission.description}
          </p>
        )}

        {/* Schedule (dayOfWeek + timeSlot) */}
        {scheduleText && (
          <div className="flex items-center gap-2.5">
            <Calendar className="w-4 h-4 text-primary shrink-0" />
            <span className="text-sm text-foreground">{scheduleText}</span>
          </div>
        )}

        {/* Deadline */}
        <div className="flex items-center gap-2.5">
          <Timer className="w-4 h-4 text-accent shrink-0" />
          <span className="text-sm text-foreground">
            기한: {mission.daysLeft === 0 ? "D-Day" : `D-${mission.daysLeft}`}
          </span>
        </div>

        {/* Confirmed status */}
        {mission.confirmedBy.length === 1 && (
          <p className="text-[11px] text-muted-foreground ml-[26px]">
            상대방 확인 대기 중
          </p>
        )}
      </div>

      {/* Actions */}
      <div className="grid grid-cols-2 gap-2 shrink-0 mt-auto">
        <Button
          variant="outline"
          size="sm"
          onClick={onExtend}
          className="rounded-full text-xs h-9"
        >
          기한 연장
        </Button>
        <Button
          size="sm"
          onClick={onComplete}
          className="rounded-full gradient-gem text-white border-0 shadow-gem text-xs h-9"
        >
          미션 완료
        </Button>
      </div>
    </div>
  )
}
