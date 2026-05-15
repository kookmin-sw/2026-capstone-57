import { apiFetch } from "./client"

export interface LocalTime {
  hour: number
  minute: number
  second: number
  nano: number
}

export type PlanEntryType = "CLASS" | "FREE" | "ACTIVITY"
export type PlanEntrySource = "MANUAL" | "SCHEDULE_AUTO" | "SCHEDULE_OVERRIDE"

export interface PlanEntryResponse {
  id: string
  date: string // "YYYY-MM-DD"
  startTime: string // "HH:mm:ss" from server
  endTime: string // "HH:mm:ss" from server
  location: string
  name: string
  type: PlanEntryType
  source: PlanEntrySource
  sourceScheduleId: string | null
}

export interface PlanEntryRequest {
  date: string // "YYYY-MM-DD"
  startTime: string // "HH:mm:ss"
  endTime: string // "HH:mm:ss"
  location?: string
  name?: string
  type: PlanEntryType
}

// 시간 문자열 "HH:mm" → "HH:mm:ss" (서버 형식)
export function toLocalTime(timeStr: string): string {
  return timeStr.length === 5 ? `${timeStr}:00` : timeStr
}

// 서버 응답 "HH:mm:ss" → "HH:mm" 표시용
export function fromLocalTime(lt: string | any): string {
  if (typeof lt === "string") {
    return lt.substring(0, 5)
  }
  // fallback: 객체 형태인 경우
  return `${String(lt.hour).padStart(2, "0")}:${String(lt.minute).padStart(2, "0")}`
}

// 날짜를 "YYYY-MM-DD" 형식으로 변환
export function formatDate(date: Date): string {
  const y = date.getFullYear()
  const m = (date.getMonth() + 1).toString().padStart(2, "0")
  const d = date.getDate().toString().padStart(2, "0")
  return `${y}-${m}-${d}`
}

/** 특정 날짜의 일정 목록 조회 */
export function getPlanEntries(date: string) {
  return apiFetch<PlanEntryResponse[]>(`/api/planner?date=${date}`)
}

/** 일정 생성 */
export function createPlanEntry(data: PlanEntryRequest) {
  return apiFetch<PlanEntryResponse>("/api/planner", {
    method: "POST",
    body: JSON.stringify(data),
  })
}

/** 일정 수정 */
export function updatePlanEntry(entryId: string, data: PlanEntryRequest) {
  return apiFetch<PlanEntryResponse>(`/api/planner/${entryId}`, {
    method: "PUT",
    body: JSON.stringify(data),
  })
}

/** 일정 삭제 */
export function deletePlanEntry(entryId: string) {
  return apiFetch<void>(`/api/planner/${entryId}`, {
    method: "DELETE",
  })
}
