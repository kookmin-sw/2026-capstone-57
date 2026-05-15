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
  QUIZ: <HelpCircle className="w-3.5 h-3.5" />,
  CHAT: <MessageCircle className="w-3.5 h-3.5" />,
  GAME: <Gamepad2 className="w-3.5 h-3.5" />,
  MISSION: <MapPin className="w-3.5 h-3.5" />,
  REVIEW: <BookHeart className="w-3.5 h-3.5" />,
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
    <div className="grid grid-cols-5 gap-1">
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
              "flex flex-col items-center gap-0.5 py-1.5 px-1 rounded-lg transition-all",
              isLocked && "opacity-50 cursor-not-allowed",
              canSelect && "cursor-pointer",
              isActive && "bg-primary/10 ring-1.5 ring-primary/50",
              !isActive && canSelect && "hover:bg-muted/50"
            )}
          >
            <div className={cn(
              "w-7 h-7 rounded-lg flex items-center justify-center transition-all",
              isCompleted && "bg-green-100 text-green-600 border border-green-200",
              isCurrent && !isCompleted && "bg-primary text-white shadow-sm",
              isLocked && "bg-muted text-muted-foreground"
            )}>
              {isCompleted ? (
                <Check className="w-3.5 h-3.5" />
              ) : isLocked ? (
                <Lock className="w-3 h-3" />
              ) : (
                stageIcons[stage]
              )}
            </div>
            <span className={cn(
              "text-[9px] font-medium leading-tight",
              isActive ? "text-primary" : "text-muted-foreground"
            )}>
              {stageLabels[stage]}
            </span>
          </button>
        )
      })}
    </div>
  )
}
