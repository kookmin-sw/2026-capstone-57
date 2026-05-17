"use client"

import { useState, useCallback } from "react"
import {
  getInteractionState,
  getMission,
  confirmMission,
  type InteractionStateDto,
} from "@/lib/api/interaction"
import { STAGE_MAP } from "./use-match-state"
import type { MissionInfo } from "@/types/match"
import type { InteractionStage } from "@/types/slot"

export interface UseMissionStageReturn {
  mission: MissionInfo | null
  handleMissionComplete: () => Promise<void>
  loadMissionData: () => Promise<void>
}

interface UseMissionStageOptions {
  matchId: string
  setInteraction: React.Dispatch<React.SetStateAction<InteractionStateDto | null>>
  setActiveStage: React.Dispatch<React.SetStateAction<InteractionStage>>
}

export function useMissionStage({
  matchId,
  setInteraction,
  setActiveStage,
}: UseMissionStageOptions): UseMissionStageReturn {
  const [mission, setMission] = useState<MissionInfo | null>(null)

  const loadMissionData = useCallback(async () => {
    try {
      const m = await getMission(matchId)
      const deadline = new Date(m.deadline)
      const now = new Date()
      const daysLeft = Math.max(0, Math.ceil((deadline.getTime() - now.getTime()) / (1000 * 60 * 60 * 24)))
      setMission({
        location: m.location,
        locationDetail: m.description || m.location,
        recommendedTime: m.activity,
        deadline: m.deadline,
        daysLeft,
      })
    } catch {
      setMission(null)
    }
  }, [matchId])

  const handleMissionComplete = useCallback(async () => {
    try {
      await confirmMission(matchId)
      const state = await getInteractionState(matchId)
      setInteraction(state)
      const nextStage = STAGE_MAP[state.currentStage] || "REVIEW"
      setActiveStage(nextStage)
    } catch (err) {
      console.error("미션 완료 실패:", err)
    }
  }, [matchId, setInteraction, setActiveStage])

  return {
    mission,
    handleMissionComplete,
    loadMissionData,
  }
}
