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
    icon: <Clock className="w-3.5 h-3.5" />,
    color: "text-muted-foreground",
    bgColor: "bg-muted",
  },
  ACTIVE: {
    icon: <Sparkles className="w-3.5 h-3.5" />,
    color: "text-primary",
    bgColor: "bg-primary/10",
  },
  COMPLETED: {
    icon: <Check className="w-3.5 h-3.5" />,
    color: "text-foreground",
    bgColor: "bg-secondary",
  },
}

const stageIcons: Record<InteractionStage, React.ReactNode> = {
  QUIZ: <BookOpen className="w-3.5 h-3.5" />,
  CHAT: <MessageCircle className="w-3.5 h-3.5" />,
  GAME: <Gamepad2 className="w-3.5 h-3.5" />,
  MISSION: <MapPin className="w-3.5 h-3.5" />,
  REVIEW: <BookOpen className="w-3.5 h-3.5" />,
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
        "hover:shadow-md transition-all duration-200",
        slot.status === "ACTIVE" && "cursor-pointer border-primary/30 bg-primary/5",
        slot.status === "EMPTY" && "border-dashed border-muted-foreground/30",
        slot.status === "COMPLETED" && "border-secondary/50 bg-secondary/10",
        className
      )}
      onClick={handleClick}
    >
      <CardContent className="p-3">
        <div className="flex items-center gap-3">
          {/* Avatar */}
          {slot.status === "ACTIVE" && slot.matchedUser ? (
            <div className="w-9 h-9 rounded-full bg-primary/20 flex items-center justify-center ring-2 ring-primary/20 shrink-0">
              <span className="text-sm">
                {slot.matchedUser.profileEmoji || slot.matchedUser.nickname.charAt(0)}
              </span>
            </div>
          ) : slot.status === "EMPTY" ? (
            <div className="w-9 h-9 rounded-full bg-muted/80 flex items-center justify-center border-2 border-dashed border-muted-foreground/20 shrink-0">
              <Clock className="w-4 h-4 text-muted-foreground/50" />
            </div>
          ) : (
            <div className="w-9 h-9 rounded-full bg-secondary/50 flex items-center justify-center shrink-0">
              <Check className="w-4 h-4 text-foreground/70" />
            </div>
          )}

          {/* Content */}
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-1.5 mb-0.5">
              {slot.status === "ACTIVE" && slot.matchedUser ? (
                <p className="font-medium text-sm text-foreground truncate">
                  {slot.matchedUser.nickname}
                </p>
              ) : slot.status === "EMPTY" ? (
                <p className="text-sm text-muted-foreground">월요일에 새로운 만남</p>
              ) : (
                <p className="text-sm text-muted-foreground">매칭 완료</p>
              )}
              {slot.isQuickMatch && (
                <Zap className="w-3 h-3 text-accent shrink-0" />
              )}
            </div>
            {slot.status === "ACTIVE" && slot.currentStage && (
              <div className="flex items-center gap-0.5 text-xs text-primary">
                {stageIcons[slot.currentStage]}
                <span>{STAGE_LABELS[slot.currentStage]}</span>
              </div>
            )}
          </div>

          {/* Status Badge with Priority */}
          <div className="flex flex-col items-center gap-0.5 shrink-0">
            <div className={cn(
              "inline-flex items-center justify-center w-8 h-8 rounded-full",
              config.bgColor,
              config.color
            )}>
              {config.icon}
            </div>
            <span className="text-[10px] text-muted-foreground">
              {PRIORITY_LABELS[slot.priority]}
            </span>
          </div>
        </div>

        {/* Mini Progress Bar for ACTIVE */}
        {slot.status === "ACTIVE" && slot.currentStage && (
          <div className="flex gap-0.5 mt-2">
            {STAGE_ORDER.map((stage, index) => (
              <div 
                key={stage}
                className={cn(
                  "h-1 flex-1 rounded-full",
                  index < currentStageIndex && "bg-primary",
                  index === currentStageIndex && "bg-primary/50",
                  index > currentStageIndex && "bg-muted"
                )}
              />
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
