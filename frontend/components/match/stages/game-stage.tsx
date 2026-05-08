"use client"

import { cn } from "@/lib/utils"
import type { GameOption } from "@/types/match"

interface GameStageProps {
  games: GameOption[]
  onSelectGame: (gameId: string) => void
}

export function GameStage({ games, onSelectGame }: GameStageProps) {
  return (
    <div className="bg-card rounded-3xl p-5 shadow-sm border border-border/30">
      {/* Header */}
      <h3 className="text-base font-semibold text-foreground mb-1">3단계 · 협동 게임</h3>
      <p className="text-sm font-medium text-foreground mb-4">함께 풀어볼 게임을 골라요</p>

      {/* Game list */}
      <div className="space-y-3">
        {games.map((game) => (
          <button
            key={game.id}
            onClick={() => onSelectGame(game.id)}
            className={cn(
              "w-full flex items-center gap-3 p-3 rounded-2xl",
              "border border-border/50 bg-card",
              "hover:border-primary/50 hover:shadow-sm transition-all",
              "active:scale-[0.98]"
            )}
          >
            <div className="w-12 h-12 rounded-2xl gradient-aurora flex items-center justify-center shrink-0">
              <span className="text-xl">{game.icon}</span>
            </div>
            <div className="flex-1 text-left">
              <p className="text-sm font-medium text-foreground">{game.name}</p>
              <p className="text-xs text-muted-foreground">{game.description}</p>
            </div>
          </button>
        ))}
      </div>
    </div>
  )
}
