"use client"

import { Button } from "@/components/ui/button"
import Image from "next/image"
import type { GameOption } from "@/types/match"

interface GameStageProps {
  games: GameOption[]
  isWaiting?: boolean
  isStarted?: boolean
  gameSessionId?: string | null
  onSelectGame: (gameId: string) => void
}

export function GameStage({ games, isWaiting = false, isStarted = false, gameSessionId, onSelectGame }: GameStageProps) {
  // 게임 시작됨 → Phaser 게임 페이지로 이동하거나 인라인 렌더링
  if (isStarted && gameSessionId) {
    return (
      <div className="h-full bg-gradient-to-br from-indigo-50 via-purple-50 to-pink-50 rounded-2xl p-4 shadow-sm border border-purple-100/50 flex flex-col items-center justify-center">
        <span className="text-3xl mb-3">🎮</span>
        <p className="text-sm font-semibold text-foreground">게임이 시작되었습니다!</p>
        <p className="text-xs text-muted-foreground mt-1">상대방과 함께 플레이하세요</p>
      </div>
    )
  }

  // 대기 중
  if (isWaiting) {
    return (
      <div className="h-full bg-gradient-to-br from-indigo-50 via-purple-50 to-pink-50 rounded-2xl p-4 shadow-sm border border-purple-100/50 flex flex-col items-center justify-center gap-3">
        <span className="text-3xl">⏳</span>
        <p className="text-sm font-semibold text-foreground">상대방을 기다리는 중...</p>
        <p className="text-xs text-muted-foreground">양쪽 모두 준비되면 게임이 시작됩니다</p>
      </div>
    )
  }

  // 게임 선택 화면
  return (
    <div className="h-full bg-gradient-to-br from-indigo-50 via-purple-50 to-pink-50 rounded-2xl p-4 shadow-sm border border-purple-100/50 flex flex-col">
      {/* Header */}
      <div className="shrink-0 mb-3">
        <h3 className="text-sm font-semibold text-foreground">3단계 · 협동 게임</h3>
        <p className="text-xs text-muted-foreground mt-0.5">함께 풀어볼 게임을 시작해요</p>
      </div>

      {/* Game - centered */}
      <div className="flex-1 flex flex-col items-center justify-center">
        {games.map((game) => (
          <div key={game.id} className="flex flex-col items-center text-center">
            <div className="w-32 h-32 rounded-2xl overflow-hidden mb-3 shadow-sm bg-white">
              <Image
                src="/images/games/달빛찾기 게임.png"
                alt={game.name}
                width={128}
                height={128}
                className="w-full h-full object-cover"
              />
            </div>
            <p className="text-base font-semibold text-foreground mb-1">{game.name}</p>
            <p className="text-xs text-muted-foreground mb-4">{game.description}</p>
            <Button
              onClick={() => onSelectGame(game.id)}
              size="sm"
              className="rounded-full gradient-gem text-white border-0 shadow-gem px-8 h-9 text-xs"
            >
              게임 시작
            </Button>
          </div>
        ))}
      </div>
    </div>
  )
}
