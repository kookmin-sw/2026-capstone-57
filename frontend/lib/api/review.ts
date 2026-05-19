import { apiFetch } from "./client"

// ─── Types ───────────────────────────────────────────────────────────────────

export type ReviewMode = "AI_ASSISTED" | "DIRECT"
export type ReviewStatus = "IN_PROGRESS" | "GENERATED" | "COMPLETED"

export interface ReviewSessionDto {
  sessionId: string
  interactionId: string
  userId: string
  mode: ReviewMode
  status: ReviewStatus
  createdAt: string
}

export interface ReviewQuestionDto {
  id: string
  question: string
  answer: string | null
  questionOrder: number
}

export interface ReviewGenerateDto {
  sessionId: string
  generatedContent: string
  suggestedSatisfaction: number
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

/** 모드 선택 → 세션 생성 */
export function selectReviewMode(matchId: string, mode: ReviewMode) {
  return apiFetch<ReviewSessionDto>(`/api/interactions/${matchId}/review/mode`, {
    method: "POST",
    body: JSON.stringify({ mode }),
  })
}

/** AI 질문 목록 조회 */
export function getReviewQuestions(matchId: string, sessionId: string) {
  return apiFetch<ReviewQuestionDto[]>(
    `/api/interactions/${matchId}/review/questions?sessionId=${sessionId}`
  )
}

/** AI 질문 답변 제출 */
export function answerReviewQuestion(
  matchId: string,
  questionId: string,
  sessionId: string,
  answer: string
) {
  return apiFetch<void>(
    `/api/interactions/${matchId}/review/questions/${questionId}/answer?sessionId=${sessionId}`,
    {
      method: "POST",
      body: JSON.stringify({ answer }),
    }
  )
}

/** AI 회고 생성 요청 */
export function generateReview(matchId: string, sessionId: string) {
  return apiFetch<ReviewGenerateDto>(
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
