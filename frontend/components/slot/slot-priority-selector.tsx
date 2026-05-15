"use client"

import { Heart, Sparkles, Star } from "lucide-react"
import { cn } from "@/lib/utils"
import {
  Drawer,
  DrawerContent,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer"
import type { SlotPriority } from "@/types/slot"
import { PRIORITY_LABELS } from "@/types/slot"

interface SlotPrioritySelectorProps {
  isOpen: boolean
  currentPriority: SlotPriority
  onSelect: (priority: SlotPriority) => void
  onClose: () => void
}

const priorities: {
  value: SlotPriority
  icon: React.ReactNode
  description: string
  examples: string
}[] = [
  {
    value: "HOBBY",
    icon: <Star className="w-5 h-5" />,
    description: "같은 취미를 가진 사람과 매칭돼요",
    examples: "운동, 음악, 영화, 게임, 독서",
  },
  {
    value: "INTEREST",
    icon: <Sparkles className="w-5 h-5" />,
    description: "비슷한 관심사를 가진 사람과 매칭돼요",
    examples: "진로, 스타트업, 예술, IT, 여행",
  },
  {
    value: "IDEAL_TYPE",
    icon: <Heart className="w-5 h-5" />,
    description: "이상형 조건에 맞는 사람과 매칭돼요",
    examples: "성격, 라이프스타일, 가치관",
  },
]

export function SlotPrioritySelector({
  isOpen,
  currentPriority,
  onSelect,
  onClose,
}: SlotPrioritySelectorProps) {
  return (
    <Drawer open={isOpen} onOpenChange={(open) => { if (!open) onClose() }}>
      <DrawerContent>
        <div className="mx-auto w-full max-w-md px-5 pb-8">
          <DrawerHeader className="px-0">
            <DrawerTitle>매칭 우선순위 설정</DrawerTitle>
          </DrawerHeader>

          <div className="space-y-2">
            {priorities.map((priority) => (
              <button
                key={priority.value}
                onClick={() => onSelect(priority.value)}
                className={cn(
                  "w-full flex items-center gap-4 p-4 rounded-2xl transition-all",
                  "border-2",
                  currentPriority === priority.value
                    ? "border-primary bg-primary/5"
                    : "border-transparent bg-muted/50 hover:bg-muted"
                )}
              >
                <div className={cn(
                  "w-10 h-10 rounded-full flex items-center justify-center",
                  currentPriority === priority.value
                    ? "bg-primary text-primary-foreground"
                    : "bg-muted text-muted-foreground"
                )}>
                  {priority.icon}
                </div>
                <div className="flex-1 text-left">
                  <p className={cn(
                    "font-medium",
                    currentPriority === priority.value ? "text-primary" : "text-foreground"
                  )}>
                    {PRIORITY_LABELS[priority.value]}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {priority.description}
                  </p>
                  <p className="text-xs text-muted-foreground/70 mt-0.5">
                    예: {priority.examples}
                  </p>
                </div>
                {currentPriority === priority.value && (
                  <div className="w-5 h-5 rounded-full bg-primary flex items-center justify-center">
                    <svg className="w-3 h-3 text-primary-foreground" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                    </svg>
                  </div>
                )}
              </button>
            ))}
          </div>
        </div>
      </DrawerContent>
    </Drawer>
  )
}
