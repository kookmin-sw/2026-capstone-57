import { apiFetch } from "./client"

export type PlanEntryType = "CLASS" | "FREE" | "ACTIVITY"
export type PlanEntrySource = "MANUAL" | "SCHEDULE_AUTO" | "SCHEDULE_OVERRIDE"

export interface PlanEntryResponse {
  id: string
  date: string
  startTime: any // LocalTime object or string from server
  endTime: any
  location: string
  name: string
  type: PlanEntryType
  source: PlanEntrySource
  sourceScheduleId: string | null
}

export interface PlanEntryRequest {
  date: string
  startTime: string // "HH:mm"
  endTime: string // "HH:mm"
  location?: string
  name?: string
  type: PlanEntryType
}

// 시간 문자열 "HH:mm" → "HH:mm" (서버 형식)
export function toLocalTime(timeStr: string): string {
  return timeStr.substring(0, 5)
}

// 서버 응답 → "HH:mm" 표시용
export function fromLocalTime(lt: any): string {
  if (!lt) return ""
  if (typeof lt === "string") return lt.substring(0, 5)
  if (typeof lt === "object" && lt.hour !== undefined) {
    return `${String(lt.hour).padStart(2, "0")}:${String(lt.minute).padStart(2, "0")}`
  }
  return ""
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

/** 주간 일정 목록 조회 (한번에) */
export function getWeeklyPlanEntries(weekStart: string) {
  return apiFetch<PlanEntryResponse[]>(`/api/planner/weekly?weekStart=${weekStart}`)
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
