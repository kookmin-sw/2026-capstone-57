"use client"

import { useState, useCallback, useEffect } from "react"
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
  missionWaiting: boolean
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
  const [missionWaiting, setMissionWaiting] = useState(false)

  const loadMissionData = useCallback(async () => {
    try {
      const m = await getMission(matchId)
      const deadline = new Date(m.deadline)
      const now = new Date()
      const daysLeft = Math.max(0, Math.ceil((deadline.getTime() - now.getTime()) / (1000 * 60 * 60 * 24)))

      const userId = localStorage.getItem("userId") || ""

      setMission({
        location: m.location,
        activity: m.activity,
        description: m.description || "",
        deadline: m.deadline,
        daysLeft,
        confirmedBy: m.confirmedBy || [],
        status: m.status,
        dayOfWeek: m.dayOfWeek || null,
        timeSlot: m.timeSlot || null,
      })

      // 내가 이미 확인했고 아직 PENDING이면 대기 상태
      if (m.status === "PENDING" && m.confirmedBy?.includes(userId)) {
        setMissionWaiting(true)
      } else if (m.status === "CONFIRMED") {
        setMissionWaiting(false)
      }
    } catch {
      setMission(null)
    }
  }, [matchId])

  const handleMissionComplete = useCallback(async () => {
    try {
      const result = await confirmMission(matchId)

      if (result.status === "CONFIRMED") {
        // 양쪽 모두 완료 → 다음 단계로
        const state = await getInteractionState(matchId)
        setInteraction(state)
        const nextStage = STAGE_MAP[state.currentStage] || "REVIEW"
        setActiveStage(nextStage)
      } else {
        // 한쪽만 완료 → 대기 상태
        setMissionWaiting(true)
        setMission((prev) =>
          prev ? { ...prev, confirmedBy: result.confirmedBy || [], status: result.status } : prev
        )
      }
    } catch (err) {
      console.error("미션 완료 실패:", err)
    }
  }, [matchId, setInteraction, setActiveStage])

  // 대기 중일 때 폴링으로 CONFIRMED 감지
  useEffect(() => {
    if (!missionWaiting) return

    const interval = setInterval(async () => {
      try {
        const m = await getMission(matchId)
        if (m.status === "CONFIRMED") {
          setMissionWaiting(false)
          const state = await getInteractionState(matchId)
          setInteraction(state)
          const nextStage = STAGE_MAP[state.currentStage] || "REVIEW"
          setActiveStage(nextStage)
        }
      } catch {
        // ignore
      }
    }, 5000)

    return () => clearInterval(interval)
  }, [missionWaiting, matchId, setInteraction, setActiveStage])

  return {
    mission,
    missionWaiting,
    handleMissionComplete,
    loadMissionData,
  }
}
