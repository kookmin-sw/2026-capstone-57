"use client"

import { useState, useEffect, useCallback } from "react"
import {
  getInteractionState,
  type InteractionStateDto,
} from "@/lib/api/interaction"
import { getSlots } from "@/lib/api/slots"
import { disconnectChatSocket } from "@/lib/chat-socket"
import { disconnectGameSocket } from "@/lib/game-socket"
import type { InteractionStage } from "@/types/slot"
import type { MatchPartner } from "@/types/match"

// 서버 stage 번호 → InteractionStage 매핑
export const STAGE_MAP: Record<number, InteractionStage> = {
  1: "QUIZ",
  2: "CHAT",
  3: "GAME",
  4: "MISSION",
  5: "REVIEW",
}

export function getCompletedStages(currentStage: number): InteractionStage[] {
  const stages: InteractionStage[] = []
  for (let i = 1; i < currentStage; i++) {
    if (STAGE_MAP[i]) stages.push(STAGE_MAP[i])
  }
  return stages
}

export interface UseMatchStateReturn {
  loading: boolean
  interaction: InteractionStateDto | null
  setInteraction: React.Dispatch<React.SetStateAction<InteractionStateDto | null>>
  partner: MatchPartner | null
  activeStage: InteractionStage
  setActiveStage: React.Dispatch<React.SetStateAction<InteractionStage>>
  currentStageNum: number
  currentStage: InteractionStage
  completedStages: InteractionStage[]
  refreshInteraction: () => Promise<InteractionStateDto | null>
}

export function useMatchState(matchId: string): UseMatchStateReturn {
  const [loading, setLoading] = useState(true)
  const [interaction, setInteraction] = useState<InteractionStateDto | null>(null)
  const [partner, setPartner] = useState<MatchPartner | null>(null)
  const [activeStage, setActiveStage] = useState<InteractionStage>("QUIZ")

  const refreshInteraction = useCallback(async () => {
    try {
      const state = await getInteractionState(matchId)
      setInteraction(state)
      return state
    } catch (err) {
      console.error("상태 갱신 실패:", err)
      return null
    }
  }, [matchId])

  // 초기 로드
  useEffect(() => {
    async function load() {
      try {
        const state = await getInteractionState(matchId)
        setInteraction(state)

        const currentStage = STAGE_MAP[state.currentStage] || "QUIZ"
        setActiveStage(currentStage)

        // 슬롯에서 매칭 상대 정보 가져오기
        try {
          const slots = await getSlots()
          const matchedSlot = slots.find((s) => s.currentMatchId === matchId)
          if (matchedSlot?.matchedUser) {
            setPartner({
              id: matchedSlot.matchedUser.userId,
              nickname: matchedSlot.matchedUser.nickname,
              profileEmoji: matchedSlot.matchedUser.nickname.charAt(0),
              department: "",
              studentYear: "",
              hobbies: [],
            })
          }
        } catch {}
      } catch (err) {
        console.error("매칭 데이터 로드 실패:", err)
      } finally {
        setLoading(false)
      }
    }
    load()

    return () => {
      disconnectChatSocket()
      disconnectGameSocket()
    }
  }, [matchId])

  const currentStageNum = interaction?.currentStage || 1
  const currentStage = STAGE_MAP[currentStageNum] || "QUIZ"
  const completedStages = getCompletedStages(currentStageNum)

  return {
    loading,
    interaction,
    setInteraction,
    partner,
    activeStage,
    setActiveStage,
    currentStageNum,
    currentStage,
    completedStages,
    refreshInteraction,
  }
}
