"use client"

import { Check, Lock, HelpCircle, MessageCircle, Gamepad2, MapPin, BookHeart } from "lucide-react"
import { cn } from "@/lib/utils"
import type { InteractionStage } from "@/types/slot"
import { STAGE_ORDER } from "@/types/slot"

interface StageStepperProps {
  currentStage: InteractionStage
  completedStages: InteractionStage[]
  activeStage: InteractionStage
  onStageSelect: (stage: InteractionStage) => void
}

const stageIcons: Record<InteractionStage, React.ReactNode> = {
  QUIZ: <HelpCircle className="w-4 h-4" />,
  CHAT: <MessageCircle className="w-4 h-4" />,
  GAME: <Gamepad2 className="w-4 h-4" />,
  MISSION: <MapPin className="w-4 h-4" />,
  REVIEW: <BookHeart className="w-4 h-4" />,
}

const stageLabels: Record<InteractionStage, string> = {
  QUIZ: "퀴즈",
  CHAT: "채팅",
  GAME: "게임",
  MISSION: "미션",
  REVIEW: "회고",
}

export function StageStepper({ currentStage, completedStages, activeStage, onStageSelect }: StageStepperProps) {
  const currentIndex = STAGE_ORDER.indexOf(currentStage)

  return (
    <div className="grid grid-cols-5 gap-1.5">
      {STAGE_ORDER.map((stage, index) => {
        const isCompleted = completedStages.includes(stage)
        const isCurrent = stage === currentStage
        const isLocked = index > currentIndex
        const isActive = stage === activeStage
        const canSelect = isCompleted || isCurrent

        return (
          <button
            key={stage}
            type="button"
            disabled={isLocked}
            onClick={(e) => {
              e.preventDefault()
              if (canSelect) onStageSelect(stage)
            }}
            className={cn(
              "flex flex-col items-center gap-1 py-2 px-1 rounded-xl transition-all",
              isLocked && "opacity-50 cursor-not-allowed",
              canSelect && "cursor-pointer",
              isActive && "bg-primary/10 ring-2 ring-primary/50",
              !isActive && canSelect && "hover:bg-muted/50"
            )}
          >
            <div className={cn(
              "w-8 h-8 rounded-xl flex items-center justify-center transition-all",
              isCompleted && "bg-green-100 text-green-600 border border-green-200",
              isCurrent && !isCompleted && "bg-primary text-white shadow-md",
              isLocked && "bg-muted text-muted-foreground"
            )}>
              {isCompleted ? (
                <Check className="w-4 h-4" />
              ) : isLocked ? (
                <Lock className="w-3.5 h-3.5" />
              ) : (
                stageIcons[stage]
              )}
            </div>
            <span className={cn(
              "text-[10px] font-medium",
              isActive ? "text-primary" : "text-foreground"
            )}>
              {index + 1}단계
            </span>
            <span className={cn(
              "text-[9px]",
              isActive ? "text-primary/80" : "text-muted-foreground"
            )}>
              {stageLabels[stage]}
            </span>
          </button>
        )
      })}
    </div>
  )
}
