"use client"

import { useState, useCallback } from "react"
import { useRouter } from "next/navigation"
import {
  getInteractionState,
  createGameSession,
  getGameSession,
  type InteractionStateDto,
} from "@/lib/api/interaction"
import {
  connectGameSocket,
  disconnectGameSocket,
  type GameEvent,
} from "@/lib/game-socket"
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

  /** 게임 클리어 후 처리: WebSocket 해제 → 상태 조회 → 해당 단계로 이동 */
  const handleGameCleared = useCallback(async () => {
    disconnectGameSocket()
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

  /** 게임 선택 핸들러 */
  const handleGameSelect = useCallback(async (gameId: string) => {
    try {
      // 1. 게임 세션 생성
      const session = await createGameSession(matchId)
      setGameSessionId(session.id)
      setGameWaiting(true)

      // 2. WebSocket 연결 (연결 시 자동으로 READY 전송)
      connectGameSocket(session.id, {
        onConnect: () => {
          console.log("게임 소켓 연결 완료")
        },
        onEvent: (event: GameEvent) => {
          if (event.type === "GAME_STARTED") {
            setGameWaiting(false)
            const token = localStorage.getItem("token") || ""
            router.push(`/game?sessionId=${session.id}&token=${token}`)
          }
          if (event.type === "GAME_CLEARED") {
            handleGameCleared()
          }
        },
        onDisconnect: () => {
          console.log("게임 소켓 연결 해제")
        },
        onGameError: (message: string) => {
          if (message.includes("찾을 수 없") || message.includes("완료된")) {
            disconnectGameSocket()
            handleGameCleared()
          }
        },
        onError: (err) => {
          console.error("게임 소켓 에러:", err)
        },
      })
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
