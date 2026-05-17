import { apiFetch } from "./client"

export type SlotPriority = "HOBBY" | "INTEREST" | "IDEAL_TYPE"
export type SlotStatus = "EMPTY" | "ACTIVE" | "COMPLETED"

export interface MatchedUserDto {
  userId: string
  nickname: string
}

export interface SlotResponseDto {
  id: string
  userId: string
  priority: SlotPriority
  currentMatchId: string | null
  isQuickMatch: boolean
  status: SlotStatus
  matchedUser: MatchedUserDto | null
}

/** 슬롯 목록 조회 */
export function getSlots() {
  return apiFetch<SlotResponseDto[]>("/api/slots")
}

/** 슬롯 우선순위 변경 */
export function updateSlotPriority(slotId: string, priority: SlotPriority) {
  return apiFetch<SlotResponseDto>(`/api/slots/${slotId}/priority/${priority}`, {
    method: "PUT",
  })
}

/** 슬롯 해금 */
export function unlockSlot() {
  return apiFetch<SlotResponseDto>("/api/slots/unlock", {
    method: "POST",
  })
}
