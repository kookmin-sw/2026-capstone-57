import type { EmotionTag } from "./diary"

// 대화 메시지 타입
export type MessageRole = "ai" | "user"

export interface ChatMessage {
  id: string
  role: MessageRole
  content: string
  timestamp: Date
}

// 대화 세션 상태
export type SessionStatus = "active" | "completing" | "completed" | "timeout"

export interface DiarySession {
  sessionId: string
  userId: string
  date: string
  messages: ChatMessage[]
  currentTurn: number
  maxTurns: number
  status: SessionStatus
}

// AI 응답
export interface AIQuestionResponse {
  question: string
  turnNumber: number
  maxTurns: number
  canComplete: boolean
}

// 컴파일된 일기
export interface CompiledDiary {
  content: string
  suggestedEmotionTag: EmotionTag
  summary: string
}

// AI 캐릭터 정보
export const AI_CHARACTER = {
  name: "해리",
  avatar: "☀️",
  greeting: "안녕! 오늘 하루는 어땠어?",
}

// 샘플 AI 질문들 (실제로는 AI가 동적으로 생성)
export const SAMPLE_AI_QUESTIONS = [
  "오늘 하루는 어떻게 보냈어?",
  "그때 기분이 어땠어?",
  "그랬구나! 그 일 말고 다른 특별한 순간이 있었어?",
  "오늘 하루를 한 단어로 표현하면 뭘 것 같아?",
  "내일은 어떤 하루가 되면 좋겠어?",
]
