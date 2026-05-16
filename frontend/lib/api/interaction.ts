import { apiFetch } from "./client"

export type StageStatus = "IN_PROGRESS" | "WAITING" | "COMPLETED" | "TERMINATED"

export interface InteractionStateDto {
  interactionId: string
  matchId: string
  currentStage: number // 1~5
  stageStatus: StageStatus
  matchCycleStart: string
  matchCycleEnd: string
  stageData: any
}

export interface QuizQuestionResponse {
  quizIndex: number
  question: string
  options: string[]
}

export interface QuizSubmitRequest {
  quizIndex: number
  answer: number
}

export interface QuizResponseDto {
  matchId: string
  quizIndex: number
  correctAnswer: number
  userAnswer: number
  isCorrect: boolean
  correctCount: number
  totalCount: number
  allCompleted: boolean
}

export interface HintQuestionDto {
  id: string
  matchId: string
  senderId: string
  responderId: string
  question: string
  answer: string | null
  status: "PENDING" | "ANSWERED"
  quizIndex: number
  createdAt: string
}

export interface MissionDto {
  id: string
  matchId: string
  location: string
  activity: string
  description: string
  deadline: string
  confirmedBy: string[]
  status: "PENDING" | "CONFIRMED" | "EXPIRED"
  selectedNodeId: string | null
}

export interface ChatSessionDto {
  sessionId: string
  matchId: string
  startTime: string
  status: "ACTIVE" | "ENDED"
  tokenLimit: number
  usedTokens: number
  icebreakerQuestion: string
}

export interface ChatMessageDto {
  messageId: string
  sessionId: string
  senderId: string
  content: string
  createdAt: string
}

/** 현재 상호작용 상태 조회 */
export function getInteractionState(matchId: string) {
  return apiFetch<InteractionStateDto>(`/api/interactions/${matchId}`)
}

/** 퀴즈 문항 조회 */
export function getQuiz(matchId: string) {
  return apiFetch<QuizQuestionResponse[]>(`/api/interactions/${matchId}/quiz`)
}

/** 퀴즈 답안 제출 */
export function submitQuiz(matchId: string, data: QuizSubmitRequest) {
  return apiFetch<QuizResponseDto>(`/api/interactions/${matchId}/quiz/submit`, {
    method: "POST",
    body: JSON.stringify(data),
  })
}



/** 힌트 질문 목록 조회 */
export function getHints(matchId: string) {
  return apiFetch<HintQuestionDto[]>(`/api/interactions/${matchId}/hints`)
}

/** 힌트 질문 전송 */
export function sendHint(matchId: string, question: string, quizIndex: number) {
  return apiFetch<HintQuestionDto>(`/api/interactions/${matchId}/hints`, {
    method: "POST",
    body: JSON.stringify({ question, quizIndex }),
  })
}

/** 힌트 답변 */
export function answerHint(matchId: string, questionId: string, answer: string) {
  return apiFetch<HintQuestionDto>(`/api/interactions/${matchId}/hints/${questionId}/answer`, {
    method: "POST",
    body: JSON.stringify({ answer }),
  })
}

/** 미션 조회 */
export function getMission(matchId: string) {
  return apiFetch<MissionDto>(`/api/missions/${matchId}`)
}

/** 미션 수행 확인 */
export function confirmMission(matchId: string) {
  return apiFetch<MissionDto>(`/api/missions/${matchId}/confirm`, {
    method: "POST",
  })
}

/** 매칭 강제 종료 */
export function terminateMatch(matchId: string, reason: string) {
  return apiFetch<void>(`/api/interactions/${matchId}/terminate`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  })
}

/** 채팅 세션 조회 */
export function getChatSession(matchId: string) {
  return apiFetch<ChatSessionDto>(`/api/chat/sessions/match/${matchId}`)
}

/** 채팅 메시지 조회 */
export function getChatMessages(sessionId: string) {
  return apiFetch<ChatMessageDto[]>(`/api/chat/sessions/${sessionId}/messages`)
}

/** 채팅 세션 생성 */
export function createChatSession(matchId: string) {
  return apiFetch<ChatSessionDto>("/api/chat/sessions", {
    method: "POST",
    body: JSON.stringify({ matchId }),
  })
}

/** 게임 세션 DTO */
export interface GameSessionDto {
  gameSessionId: string
  matchId: string
  status: "WAITING" | "PLAYING" | "FINISHED"
}

/** 게임 세션 생성 */
export function createGameSession(matchId: string) {
  return apiFetch<GameSessionDto>(`/api/v1/matches/${matchId}/game-sessions`, {
    method: "POST",
  })
}
