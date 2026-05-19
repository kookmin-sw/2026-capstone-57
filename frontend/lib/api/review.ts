import { apiFetch } from "./client"

// ─── Types ───────────────────────────────────────────────────────────────────

export type ReviewMode = "AI_ASSISTED" | "DIRECT"
export type ReviewStatus = "IN_PROGRESS" | "GENERATED" | "COMPLETED"

export interface ConversationTurn {
  turnNumber: number
  question: string
  answer: string
}

export interface ReviewSessionResponse {
  sessionId: string
  interactionId: string
  userId: string
  mode: ReviewMode
  status: ReviewStatus
  currentQuestion: string | null
  maxTurns: number
  conversationHistory: ConversationTurn[]
  createdAt: string
}

export interface ReviewAnswerResponse {
  sessionId: string
  question: string | null
  isConversationComplete: boolean
  currentTurn: number
  maxTurns: number
  conversationHistory: ConversationTurn[]
}

export interface ReviewGenerateResponse {
  sessionId: string
  generatedContent: string
  suggestedSatisfaction: number
  generatedAt: string
}

export interface ReviewConfirmRequest {
  reflection: string
  satisfaction: number
  wantToMeetAgain: boolean
}

export interface ReviewDirectRequest {
  satisfaction: number
  reflection: string
  wantToMeetAgain: boolean
}

export interface ReviewResultDto {
  sessionId: string
  mode: ReviewMode
  status: ReviewStatus
  reflection: string
  satisfaction: number
  wantToMeetAgain: boolean
  createdAt: string
}

// ─── API Functions ───────────────────────────────────────────────────────────

/** 기존 회고 조회 (204 = 미작성) */
export function getReview(matchId: string) {
  return apiFetch<ReviewResultDto | undefined>(`/api/interactions/${matchId}/review`)
}

/** 모드 선택 → 세션 생성 + 첫 질문 반환 */
export function selectReviewMode(matchId: string, mode: ReviewMode) {
  return apiFetch<ReviewSessionResponse>(`/api/interactions/${matchId}/review/mode`, {
    method: "POST",
    body: JSON.stringify({ mode }),
  })
}

/** 답변 제출 → 다음 질문 반환 (멀티턴) */
export function submitReviewAnswer(matchId: string, sessionId: string, answer: string) {
  return apiFetch<ReviewAnswerResponse>(
    `/api/interactions/${matchId}/review/answer?sessionId=${sessionId}`,
    {
      method: "POST",
      body: JSON.stringify({ answer }),
    }
  )
}

/** AI 회고 생성 요청 */
export function generateReview(matchId: string, sessionId: string) {
  return apiFetch<ReviewGenerateResponse>(
    `/api/interactions/${matchId}/review/generate?sessionId=${sessionId}`,
    { method: "POST" }
  )
}

/** AI 회고 확정 */
export function confirmReview(matchId: string, sessionId: string, data: ReviewConfirmRequest) {
  return apiFetch<void>(
    `/api/interactions/${matchId}/review/confirm?sessionId=${sessionId}`,
    {
      method: "POST",
      body: JSON.stringify(data),
    }
  )
}

/** 직접 작성 회고 제출 */
export function submitDirectReview(matchId: string, data: ReviewDirectRequest) {
  return apiFetch<void>(`/api/interactions/${matchId}/review/direct`, {
    method: "POST",
    body: JSON.stringify(data),
  })
}
