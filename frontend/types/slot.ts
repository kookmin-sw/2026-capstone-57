// 슬롯 상태
export type SlotStatus = "EMPTY" | "ACTIVE" | "COMPLETED"

// 슬롯 우선순위 (매칭 기준)
export type SlotPriority = "HOBBY" | "INTEREST" | "IDEAL_TYPE"

// 상호작용 단계 (ACTIVE 상태에서의 진행 흐름)
export type InteractionStage = "QUIZ" | "CHAT" | "GAME" | "MISSION" | "REVIEW"

// 매칭된 사용자 정보
export interface MatchedUser {
  id: string
  nickname: string
  profileEmoji?: string
}

// 슬롯 데이터
export interface Slot {
  id: string
  userId: string
  slotNumber: number
  priority: SlotPriority
  currentMatchId: string | null
  isQuickMatch: boolean
  status: SlotStatus
  matchedUser: MatchedUser | null
  // ACTIVE 상태 전용 필드
  currentStage?: InteractionStage
  stageProgress?: number // 0-100
  daysRemaining?: number
  // EMPTY 상태 전용 필드
  nextMatchDate?: string // ISO date string
}

// 잠긴 슬롯 정보
export interface LockedSlot {
  slotNumber: number
  requiredLevel: number
  currentLevel: number
}

// 우선순위 한글 라벨
export const PRIORITY_LABELS: Record<SlotPriority, string> = {
  HOBBY: "취미",
  INTEREST: "관심사",
  IDEAL_TYPE: "이상형",
}

// 우선순위 설명
export const PRIORITY_DESCRIPTIONS: Record<SlotPriority, string> = {
  HOBBY: "운동, 음악, 영화, 게임 등 취미 기반",
  INTEREST: "진로, 스타트업, 예술, 여행 등 관심사 기반",
  IDEAL_TYPE: "성격, 라이프스타일, 가치관 기반",
}

// 상태 한글 라벨
export const STATUS_LABELS: Record<SlotStatus, string> = {
  EMPTY: "대기 중",
  ACTIVE: "진행 중",
  COMPLETED: "완료",
}

// 단계 한글 라벨
export const STAGE_LABELS: Record<InteractionStage, string> = {
  QUIZ: "퀴즈",
  CHAT: "채팅",
  GAME: "게임",
  MISSION: "미션",
  REVIEW: "회고",
}

// 단계 순서
export const STAGE_ORDER: InteractionStage[] = ["QUIZ", "CHAT", "GAME", "MISSION", "REVIEW"]
