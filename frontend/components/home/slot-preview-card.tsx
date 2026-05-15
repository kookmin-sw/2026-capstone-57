"use client"

import { useRouter } from "next/navigation"
import {
  Clock, Sparkles, Check, Zap,
  MessageCircle, Gamepad2, MapPin, BookOpen
} from "lucide-react"
import { cn } from "@/lib/utils"
import { Card, CardContent } from "@/components/ui/card"
import type { Slot, SlotStatus, InteractionStage } from "@/types/slot"
import { PRIORITY_LABELS, STAGE_LABELS, STAGE_ORDER } from "@/types/slot"

interface SlotPreviewCardProps {
  slot: Slot
  className?: string
}

const statusConfig: Record<SlotStatus, {
  icon: React.ReactNode
  color: string
  bgColor: string
}> = {
  EMPTY: {
    icon: <Clock className="w-3 h-3" />,
    color: "text-muted-foreground",
    bgColor: "bg-muted",
  },
  ACTIVE: {
    icon: <Sparkles className="w-3 h-3" />,
    color: "text-primary",
    bgColor: "bg-primary/10",
  },
  COMPLETED: {
    icon: <Check className="w-3 h-3" />,
    color: "text-foreground",
    bgColor: "bg-secondary",
  },
}

const stageIcons: Record<InteractionStage, React.ReactNode> = {
  QUIZ: <BookOpen className="w-3 h-3" />,
  CHAT: <MessageCircle className="w-3 h-3" />,
  GAME: <Gamepad2 className="w-3 h-3" />,
  MISSION: <MapPin className="w-3 h-3" />,
  REVIEW: <BookOpen className="w-3 h-3" />,
}

export function SlotPreviewCard({ slot, className }: SlotPreviewCardProps) {
  const router = useRouter()
  const config = statusConfig[slot.status]
  const currentStageIndex = slot.currentStage
    ? STAGE_ORDER.indexOf(slot.currentStage)
    : 0

  const handleClick = () => {
    if (slot.status === "ACTIVE" && slot.currentMatchId) {
      router.push(`/match/${slot.currentMatchId}`)
    }
  }

  return (
    <Card
      className={cn(
        "border",
        "hover:shadow-sm transition-all duration-200",
        slot.status === "ACTIVE" && "cursor-pointer border-primary/30 bg-primary/5",
        slot.status === "EMPTY" && "border-dashed border-muted-foreground/30",
        slot.status === "COMPLETED" && "border-secondary/50 bg-secondary/10",
        className
      )}
      onClick={handleClick}
    >
      <CardContent className="px-3 py-2">
        <div className="flex items-center gap-2.5">
          {/* Avatar - smaller */}
          {slot.status === "ACTIVE" && slot.matchedUser ? (
            <div className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center ring-1 ring-primary/20 shrink-0">
              <span className="text-sm">
                {slot.matchedUser.profileEmoji || slot.matchedUser.nickname.charAt(0)}
              </span>
            </div>
          ) : slot.status === "EMPTY" ? (
            <div className="w-8 h-8 rounded-full bg-muted/80 flex items-center justify-center border border-dashed border-muted-foreground/20 shrink-0">
              <Clock className="w-3.5 h-3.5 text-muted-foreground/50" />
            </div>
          ) : (
            <div className="w-8 h-8 rounded-full bg-secondary/50 flex items-center justify-center shrink-0">
              <Check className="w-3.5 h-3.5 text-foreground/70" />
            </div>
          )}

          {/* Content */}
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-1.5">
              {slot.status === "ACTIVE" && slot.matchedUser ? (
                <p className="font-medium text-xs text-foreground truncate">
                  {slot.matchedUser.nickname}
                </p>
              ) : slot.status === "EMPTY" ? (
                <p className="text-xs text-muted-foreground">월요일에 새로운 만남</p>
              ) : (
                <p className="text-xs text-muted-foreground">매칭 완료</p>
              )}
              {slot.isQuickMatch && (
                <Zap className="w-2.5 h-2.5 text-accent shrink-0" />
              )}
            </div>
            {slot.status === "ACTIVE" && slot.currentStage && (
              <div className="flex items-center gap-1 mt-0.5">
                <div className="flex items-center gap-0.5 text-[11px] text-primary">
                  {stageIcons[slot.currentStage]}
                  <span>{STAGE_LABELS[slot.currentStage]}</span>
                </div>
                {/* Inline mini progress */}
                <div className="flex gap-0.5 flex-1 ml-1">
                  {STAGE_ORDER.map((stage, index) => (
                    <div
                      key={stage}
                      className={cn(
                        "h-0.5 flex-1 rounded-full",
                        index < currentStageIndex && "bg-primary",
                        index === currentStageIndex && "bg-primary/50",
                        index > currentStageIndex && "bg-muted"
                      )}
                    />
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Status Badge */}
          <div className="flex items-center gap-1 shrink-0">
            <span className="text-[10px] text-muted-foreground">
              {PRIORITY_LABELS[slot.priority]}
            </span>
            <div className={cn(
              "w-6 h-6 rounded-full flex items-center justify-center",
              config.bgColor,
              config.color
            )}>
              {config.icon}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
