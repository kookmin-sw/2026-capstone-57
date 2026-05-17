"use client"

import { MapPin, Clock, Timer } from "lucide-react"
import { Button } from "@/components/ui/button"
import type { MissionInfo } from "@/types/match"

interface MissionStageProps {
  mission: MissionInfo
  onExtend: () => void
  onComplete: () => void
}

export function MissionStage({ mission, onExtend, onComplete }: MissionStageProps) {
  return (
    <div className="h-full bg-gradient-to-br from-sky-50 via-indigo-50 to-violet-50 rounded-2xl p-4 shadow-sm border border-indigo-100/50 flex flex-col">
      {/* Header */}
      <div className="shrink-0">
        <h3 className="text-sm font-semibold text-foreground">4단계 · 오프라인 미션</h3>
        <h4 className="text-base font-semibold text-foreground mt-1">
          {mission.location}에서 만나요
        </h4>
        <p className="text-xs text-muted-foreground mt-0.5">
          두 분의 동선이 자연스럽게 겹치는 시간을 찾았어요.
        </p>
      </div>

      {/* Info panel */}
      <div className="flex-1 flex items-center my-1">
        <div className="w-full bg-white/80 rounded-xl p-4 space-y-3 border border-indigo-100/50">
          <div className="flex items-center gap-2">
            <MapPin className="w-4 h-4 text-primary shrink-0" />
            <span className="text-sm text-foreground">{mission.locationDetail}</span>
          </div>
          <div className="flex items-center gap-2">
            <Clock className="w-4 h-4 text-primary shrink-0" />
            <span className="text-sm text-foreground">추천: {mission.recommendedTime}</span>
          </div>
          <div className="flex items-center gap-2">
            <Timer className="w-4 h-4 text-accent shrink-0" />
            <span className="text-sm text-foreground">기한: D-{mission.daysLeft}</span>
          </div>
        </div>
      </div>

      {/* Actions */}
      <div className="grid grid-cols-2 gap-2 shrink-0">
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
