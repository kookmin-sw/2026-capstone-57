import { apiFetch } from "./client"

export type EmotionTag = "HAPPY" | "SAD" | "ANGRY" | "ANXIOUS" | "CALM" | "EXCITED" | "TIRED"
export type DiarySource = "MANUAL" | "AI_GENERATED"

export interface DiaryEntryResponse {
  id: string
  userId: string
  entryDate: string
  content: string
  emotionTag: EmotionTag
  source: DiarySource
  aiSessionId: string | null
  streakCount: number
  createdAt: string
}

export interface DiaryInputDto {
  content: string
  emotionTag?: EmotionTag
  date: string
  source?: DiarySource
  aiSessionId?: string
}

export interface StreakInfoDto {
  currentStreak: number
  longestStreak: number
}

export interface DiarySessionResponse {
  sessionId: string
  userId: string
  targetDate: string
  status: "IN_PROGRESS" | "READY_TO_GENERATE" | "GENERATED" | "COMPLETED" | "CANCELLED"
  currentQuestion: string | null
  currentTurnNumber: number
  maxTurns: number
  conversationHistory: { turnNumber: number; question: string; answer: string }[]
}

export interface DiaryTurnResponse {
  sessionId: string
  isCompleted: boolean
  nextQuestion: string | null
  currentTurnNumber: number
  maxTurns: number
}

export interface GeneratedDiaryPreview {
  sessionId: string
  generatedContent: string
  suggestedEmotion: EmotionTag
  generatedAt: string
}

export interface Pageable {
  page?: number
  size?: number
}

/** 일기 목록 조회 */
export function getDiaryEntries(pageable?: Pageable) {
  const params = new URLSearchParams()
  if (pageable?.page !== undefined) params.set("page", String(pageable.page))
  if (pageable?.size !== undefined) params.set("size", String(pageable.size))
  return apiFetch<{ content: DiaryEntryResponse[]; totalElements: number; totalPages: number }>(
    `/api/diary?${params.toString()}`
  )
}

/** 일기 작성 */
export function createDiary(data: DiaryInputDto) {
  return apiFetch<DiaryEntryResponse>("/api/diary", {
    method: "POST",
    body: JSON.stringify(data),
  })
}

/** 연속 작성 일수 조회 */
export function getDiaryStreak() {
  return apiFetch<StreakInfoDto>("/api/diary/streak")
}

/** AI 일기 세션 시작 */
export function startDiarySession(date: string) {
  return apiFetch<DiarySessionResponse>(`/api/diary/sessions/start?date=${date}`, {
    method: "POST",
  })
}

/** 활성 세션 조회 */
export function getActiveSession(date: string) {
  return apiFetch<DiarySessionResponse>(`/api/diary/sessions/active?date=${date}`)
}

/** 질문에 답변 */
export function answerDiaryQuestion(sessionId: string, answer: string) {
  return apiFetch<DiaryTurnResponse>(`/api/diary/sessions/${sessionId}/answer`, {
    method: "POST",
    body: JSON.stringify({ answer }),
  })
}

/** 일기 생성 요청 */
export function generateDiary(sessionId: string) {
  return apiFetch<GeneratedDiaryPreview>(`/api/diary/sessions/${sessionId}/generate`, {
    method: "POST",
  })
}

/** 일기 확정 */
export function confirmDiary(sessionId: string, editedContent?: string, emotionTag?: EmotionTag) {
  return apiFetch<DiaryEntryResponse>(`/api/diary/sessions/${sessionId}/confirm`, {
    method: "POST",
    body: JSON.stringify({ editedContent, emotionTag }),
  })
}

/** 세션 취소 */
export function cancelDiarySession(sessionId: string) {
  return apiFetch<void>(`/api/diary/sessions/${sessionId}/cancel`, {
    method: "POST",
  })
}
