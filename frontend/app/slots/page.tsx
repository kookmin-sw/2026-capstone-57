"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { AppShell } from "@/components/app-shell"
import { SlotList } from "@/components/slot/slot-list"
import { getSlots, updateSlotPriority, unlockSlot, type SlotResponseDto } from "@/lib/api/slots"
import type { Slot, SlotPriority, LockedSlot } from "@/types/slot"

// API 응답 → 프론트 Slot 타입 변환
function toSlot(dto: SlotResponseDto, index: number): Slot {
  return {
    id: dto.id,
    userId: dto.userId,
    slotNumber: index + 1,
    priority: dto.priority,
    currentMatchId: dto.currentMatchId,
    isQuickMatch: dto.isQuickMatch,
    status: dto.status,
    matchedUser: dto.matchedUser
      ? {
          id: dto.matchedUser.userId,
          nickname: dto.matchedUser.nickname,
          profileEmoji: dto.matchedUser.nickname.charAt(0),
        }
      : null,
    currentStage: undefined,
    stageProgress: undefined,
    daysRemaining: undefined,
  }
}

export default function SlotsPage() {
  const router = useRouter()
  const [slots, setSlots] = useState<Slot[]>([])
  const [loading, setLoading] = useState(true)

  const loadSlots = async () => {
    try {
      setLoading(true)
      const data = await getSlots()
      setSlots(data.map(toSlot))
    } catch (err) {
      console.error("슬롯 조회 실패:", err)
      setSlots([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadSlots()
  }, [])

  const handlePriorityChange = async (slotId: string, priority: SlotPriority) => {
    try {
      await updateSlotPriority(slotId, priority)
      setSlots((prev) =>
        prev.map((slot) =>
          slot.id === slotId ? { ...slot, priority } : slot
        )
      )
    } catch (err) {
      console.error("우선순위 변경 실패:", err)
    }
  }

  const handleSlotClick = (slot: Slot) => {
    if (slot.status === "ACTIVE" && slot.currentMatchId) {
      router.push(`/match/${slot.currentMatchId}`)
    }
  }

  const handleUnlockSlot = async (lockedSlot: LockedSlot) => {
    if (lockedSlot.currentLevel < lockedSlot.requiredLevel) {
      alert(`레벨 ${lockedSlot.requiredLevel}이 필요합니다. 현재 레벨: ${lockedSlot.currentLevel}`)
      return
    }
    try {
      await unlockSlot()
      loadSlots()
    } catch (err) {
      console.error("슬롯 해금 실패:", err)
    }
  }

  return (
    <AppShell title="내 슬롯" showBackButton>
      <div className="px-4 py-4">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <p className="text-sm text-muted-foreground">로딩 중...</p>
          </div>
        ) : (
          <SlotList
            slots={slots}
            onPriorityChange={handlePriorityChange}
            onSlotClick={handleSlotClick}
          />
        )}

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
