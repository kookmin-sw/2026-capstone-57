"use client"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import Image from "next/image"
import type { GameOption } from "@/types/match"

interface GameStageProps {
  games: GameOption[]
  onSelectGame: (gameId: string) => void
}

export function GameStage({ games, onSelectGame }: GameStageProps) {
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
