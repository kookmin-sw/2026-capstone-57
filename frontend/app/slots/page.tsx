"use client"

import { useState } from "react"
import { AppShell } from "@/components/app-shell"
import { SlotList, sampleSlots, sampleLockedSlots } from "@/components/slot/slot-list"
import type { Slot, SlotPriority, LockedSlot } from "@/types/slot"

export default function SlotsPage() {
  const [slots, setSlots] = useState<Slot[]>(sampleSlots)

  const handlePriorityChange = (slotId: string, priority: SlotPriority) => {
    setSlots((prev) =>
      prev.map((slot) =>
        slot.id === slotId ? { ...slot, priority } : slot
      )
    )
    // TODO: API call to update priority
    // PUT /api/slots/{slotId}/priority/{priority}
  }

  const handleSlotClick = (slot: Slot) => {
    if (slot.status === "ACTIVE") {
      // TODO: Navigate to interaction page
      console.log("[v0] Navigating to slot interaction:", slot.id)
    }
  }

  const handleUnlockSlot = (lockedSlot: LockedSlot) => {
    // TODO: API call to unlock new slot
    // POST /api/slots/unlock
    if (lockedSlot.currentLevel >= lockedSlot.requiredLevel) {
      alert("슬롯이 해금되었습니다!")
    } else {
      alert(`레벨 ${lockedSlot.requiredLevel}이 필요합니다. 현재 레벨: ${lockedSlot.currentLevel}`)
    }
  }

  return (
    <AppShell title="내 슬롯" showBackButton>
      <div className="px-4 py-4">

        {/* Slot List */}
        <SlotList
          slots={slots}
          lockedSlots={sampleLockedSlots}
          onPriorityChange={handlePriorityChange}
          onSlotClick={handleSlotClick}
          onUnlockSlot={handleUnlockSlot}
        />

        {/* Info Card */}
        <div className="mt-6 p-4 rounded-2xl bg-muted/50 border border-border">
          <h3 className="font-medium text-foreground mb-2">
            슬롯 매칭 안내
          </h3>
          <ul className="text-sm text-muted-foreground space-y-1.5">
            <li className="flex items-start gap-2">
              <span className="text-primary">•</span>
              <span>매주 월요일 자정에 새로운 상대가 배정됩니다</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-primary">•</span>
              <span>퀴즈 → 채팅 → 게임 → 미션 → 회고 순으로 진행됩니다</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-primary">•</span>
              <span>우선순위를 설정하면 더 맞는 상대를 만날 수 있어요</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-primary">•</span>
              <span>레벨업을 통해 더 많은 슬롯을 해금할 수 있어요</span>
            </li>
          </ul>
        </div>
      </div>
    </AppShell>
  )
}
