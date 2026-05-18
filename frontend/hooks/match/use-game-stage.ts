"use client"

import { useState, useCallback } from "react"
import { useRouter } from "next/navigation"
import {
  getInteractionState,
  createGameSession,
  getGameSession,
  type InteractionStateDto,
} from "@/lib/api/interaction"
import { STAGE_MAP } from "./use-match-state"
import type { InteractionStage } from "@/types/slot"

export interface UseGameStageReturn {
  gameSessionId: string | null
  gameWaiting: boolean
  handleGameSelect: (gameId: string) => Promise<void>
  handleGameCleared: () => Promise<void>
  loadGameData: () => Promise<void>
}

interface UseGameStageOptions {
  matchId: string
  setInteraction: React.Dispatch<React.SetStateAction<InteractionStateDto | null>>
  setActiveStage: React.Dispatch<React.SetStateAction<InteractionStage>>
  loadStageData: (stage: InteractionStage) => Promise<void>
}

export function useGameStage({
  matchId,
  setInteraction,
  setActiveStage,
  loadStageData,
}: UseGameStageOptions): UseGameStageReturn {
  const router = useRouter()
  const [gameSessionId, setGameSessionId] = useState<string | null>(null)
  const [gameWaiting, setGameWaiting] = useState(false)

  /** 게임 클리어 후 처리: 상태 조회 → 해당 단계로 이동 */
  const handleGameCleared = useCallback(async () => {
    setGameWaiting(false)
    try {
      const state = await getInteractionState(matchId)
      setInteraction(state)
      const nextStage = STAGE_MAP[state.currentStage] || "MISSION"
      setActiveStage(nextStage)
      await loadStageData(nextStage)
    } catch (err) {
      console.error("게임 클리어 후 상태 조회 실패:", err)
    }
  }, [matchId, setInteraction, setActiveStage, loadStageData])

  /** 게임 선택 핸들러 — 세션 생성 후 바로 게임 페이지로 이동 */
  const handleGameSelect = useCallback(async (gameId: string) => {
    try {
      const session = await createGameSession(matchId)
      setGameSessionId(session.id)
      const token = localStorage.getItem("token") || ""
      router.push(`/game?sessionId=${session.id}&token=${token}`)
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : String(err)
      if (errorMessage.includes("400") || errorMessage.includes("완료된")) {
        await handleGameCleared()
      } else {
        console.error("게임 세션 생성 실패:", err)
      }
      setGameWaiting(false)
    }
  }, [matchId, router, handleGameCleared])

  /** 게임 단계 데이터 로드 (기존 세션 복원) */
  const loadGameData = useCallback(async () => {
    try {
      const session = await getGameSession(matchId)
      setGameSessionId(session.id)

      if (session.status === "FINISHED") {
        await handleGameCleared()
      } else if (session.status === "PLAYING") {
        const token = localStorage.getItem("token") || ""
        router.push(`/game?sessionId=${session.id}&token=${token}`)
      } else if (session.status === "WAITING") {
        // WAITING 세션이 있지만 자동 복원하지 않음 — 사용자가 게임 시작 버튼을 눌러야 함
        // sessionId만 저장해두고 게임 선택 화면 유지
        setGameSessionId(session.id)
        setGameWaiting(false)
      }
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : String(err)
      if (errorMessage.includes("400") || errorMessage.includes("완료된")) {
        await handleGameCleared()
      } else {
        setGameSessionId(null)
        setGameWaiting(false)
      }
    }
  }, [matchId, router, handleGameCleared])

  return {
    gameSessionId,
    gameWaiting,
    handleGameSelect,
    handleGameCleared,
    loadGameData,
  }
}
