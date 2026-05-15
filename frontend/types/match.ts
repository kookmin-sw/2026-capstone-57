import type { InteractionStage } from "./slot"

// 매칭 상대 상세 정보
export interface MatchPartner {
  id: string
  nickname: string
  profileEmoji: string
  department: string
  studentYear: string
  hobbies: string[]
}

// 매칭 데이터
export interface Match {
  id: string
  partnerId: string
  partner: MatchPartner
  currentStage: InteractionStage
  completedStages: InteractionStage[]
  intimacy: number // 0-100
  createdAt: string
}

// 퀴즈 문제
export interface QuizQuestion {
  id: string
  question: string
  options: string[]
  correctIndex: number
}

// 채팅 메시지
export interface ChatMessage {
  id: string
  senderId: string
  content: string
  timestamp: string
  isMe: boolean
}

// 게임 옵션
export interface GameOption {
  id: string
  name: string
  description: string
  icon: string
}

// 미션 정보
export interface MissionInfo {
  location: string
  locationDetail: string
  recommendedTime: string
  deadline: string
  daysLeft: number
}

// 회고 데이터
export interface ReflectionData {
  mode: "ai" | "free"
  content: string
  rating?: number
}
