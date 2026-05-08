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
    <div className="gradient-aurora rounded-3xl p-5 shadow-glow">
      {/* Header */}
      <h3 className="text-base font-semibold text-foreground mb-2">4단계 · 오프라인 미션</h3>
      
      {/* Title */}
      <h4 className="text-lg font-semibold text-foreground mb-1">
        {mission.location}에서 만나요
      </h4>
      <p className="text-sm text-muted-foreground mb-4">
        두 분의 동선이 자연스럽게 겹치는 시간을 찾았어요.
      </p>

      {/* Info panel */}
      <div className="bg-background/40 rounded-2xl p-4 space-y-3 mb-4">
        <div className="flex items-center gap-2">
          <MapPin className="w-4 h-4 text-primary" />
          <span className="text-sm text-foreground">
            장소: {mission.locationDetail}
          </span>
        </div>
        <div className="flex items-center gap-2">
          <Clock className="w-4 h-4 text-primary" />
          <span className="text-sm text-foreground">
            추천 시간: {mission.recommendedTime}
          </span>
        </div>
        <div className="flex items-center gap-2">
          <Timer className="w-4 h-4 text-accent" />
          <span className="text-sm text-foreground">
            미션 기한: D-{mission.daysLeft}
          </span>
        </div>
      </div>

      {/* Actions */}
      <div className="grid grid-cols-2 gap-3">
        <Button 
          variant="outline" 
          onClick={onExtend}
          className="rounded-full"
        >
          기한 연장
        </Button>
        <Button 
          onClick={onComplete}
          className="rounded-full gradient-gem text-white border-0 shadow-gem"
        >
          미션 완료
        </Button>
      </div>
    </div>
  )
}
