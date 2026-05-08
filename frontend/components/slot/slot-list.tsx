"use client"

import { SlotCard, LockedSlotCard } from "./slot-card"
import type { Slot, SlotPriority, LockedSlot } from "@/types/slot"

interface SlotListProps {
  slots: Slot[]
  lockedSlots?: LockedSlot[]
  onPriorityChange?: (slotId: string, priority: SlotPriority) => void
  onSlotClick?: (slot: Slot) => void
  onUnlockSlot?: (lockedSlot: LockedSlot) => void
}

export function SlotList({ 
  slots, 
  lockedSlots = [],
  onPriorityChange,
  onSlotClick,
  onUnlockSlot 
}: SlotListProps) {
  return (
    <div className="space-y-4">
      {slots.map((slot) => (
        <SlotCard
          key={slot.id}
          slot={slot}
          onPriorityChange={onPriorityChange}
          onSlotClick={onSlotClick}
        />
      ))}
      
      {/* Locked slots */}
      {lockedSlots.map((lockedSlot) => (
        <LockedSlotCard
          key={`locked-${lockedSlot.slotNumber}`}
          lockedSlot={lockedSlot}
          onUnlock={() => onUnlockSlot?.(lockedSlot)}
        />
      ))}
    </div>
  )
}

// Sample data for development/preview
export const sampleSlots: Slot[] = [
  {
    id: "1",
    userId: "user-1",
    slotNumber: 1,
    priority: "HOBBY",
    currentMatchId: "match-1",
    isQuickMatch: false,
    status: "ACTIVE",
    matchedUser: {
      id: "matched-user-1",
      nickname: "하늘빛",
      profileEmoji: "🌤️",
    },
    currentStage: "CHAT",
    stageProgress: 40,
    daysRemaining: 5,
  },
  {
    id: "2",
    userId: "user-1",
    slotNumber: 2,
    priority: "INTEREST",
    currentMatchId: null,
    isQuickMatch: false,
    status: "EMPTY",
    matchedUser: null,
  },
  {
    id: "3",
    userId: "user-1",
    slotNumber: 3,
    priority: "IDEAL_TYPE",
    currentMatchId: "match-2",
    isQuickMatch: true,
    status: "COMPLETED",
    matchedUser: {
      id: "matched-user-2",
      nickname: "별빛",
      profileEmoji: "✨",
    },
  },
]

export const sampleLockedSlots: LockedSlot[] = [
  {
    slotNumber: 4,
    requiredLevel: 5,
    currentLevel: 3,
  },
]
