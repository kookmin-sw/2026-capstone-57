// 요일 타입
export type DayOfWeek = 0 | 1 | 2 | 3 | 4 | 5 | 6

// 일정 유형
export type EntryType = "CLASS" | "FREE" | "ACTIVITY"

// 시간표 항목
export interface TimetableEntry {
  id: string
  userId: string
  dayOfWeek: DayOfWeek
  startTime: string // "HH:mm"
  endTime: string // "HH:mm"
  location: string
  courseName: string
  type: EntryType
  color?: string
}

// 일일 플래너 일정 항목
export interface PlanEntry {
  id: string
  startTime: string // "HH:mm"
  endTime: string // "HH:mm"
  location: string
  activity: string
  type: EntryType
}

// 일일 플래너
export interface DailyPlan {
  id: string
  userId: string
  entryDate: string // "YYYY-MM-DD"
  entries: PlanEntry[]
}

// 요일 라벨
export const DAY_LABELS: Record<DayOfWeek, string> = {
  0: "일",
  1: "월",
  2: "화",
  3: "수",
  4: "목",
  5: "금",
  6: "토",
}

// 일정 유형 라벨
export const ENTRY_TYPE_LABELS: Record<EntryType, string> = {
  CLASS: "수업",
  FREE: "자유시간",
  ACTIVITY: "활동",
}

// 일정 유형별 색상
export const ENTRY_TYPE_COLORS: Record<EntryType, string> = {
  CLASS: "bg-primary/20 border-primary/30 text-primary",
  FREE: "bg-emerald-100 border-emerald-200 text-emerald-700",
  ACTIVITY: "bg-accent/20 border-accent/30 text-accent",
}

// 시간표 색상 팔레트
export const COURSE_COLORS = [
  "bg-sky-100 border-sky-200 text-sky-700",
  "bg-rose-100 border-rose-200 text-rose-700",
  "bg-amber-100 border-amber-200 text-amber-700",
  "bg-emerald-100 border-emerald-200 text-emerald-700",
  "bg-violet-100 border-violet-200 text-violet-700",
  "bg-orange-100 border-orange-200 text-orange-700",
]
