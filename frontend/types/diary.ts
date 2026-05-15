// 감정 태그
export type EmotionTag = "HAPPY" | "SAD" | "ANGRY" | "ANXIOUS" | "CALM" | "EXCITED" | "TIRED"

// 일기 항목
export interface DiaryEntry {
  id: string
  userId: string
  entryDate: string // "YYYY-MM-DD"
  content: string
  emotionTag: EmotionTag
  streakCount: number
  createdAt: string
  updatedAt: string
}

// 스트릭 정보
export interface StreakInfo {
  currentStreak: number
  longestStreak: number
}

// 감정 메타 정보
export interface EmotionMeta {
  label: string
  emoji: string
  weatherIcon: string
  color: string
  bgColor: string
}

// 감정 라벨 및 아이콘
export const EMOTION_META: Record<EmotionTag, EmotionMeta> = {
  HAPPY: {
    label: "행복해요",
    emoji: "😊",
    weatherIcon: "☀️",
    color: "text-amber-500",
    bgColor: "bg-amber-50",
  },
  SAD: {
    label: "슬퍼요",
    emoji: "😢",
    weatherIcon: "🌧️",
    color: "text-blue-500",
    bgColor: "bg-blue-50",
  },
  ANGRY: {
    label: "화나요",
    emoji: "😤",
    weatherIcon: "⛈️",
    color: "text-red-500",
    bgColor: "bg-red-50",
  },
  ANXIOUS: {
    label: "불안해요",
    emoji: "😰",
    weatherIcon: "🌫️",
    color: "text-gray-500",
    bgColor: "bg-gray-50",
  },
  CALM: {
    label: "평온해요",
    emoji: "😌",
    weatherIcon: "🌤️",
    color: "text-teal-500",
    bgColor: "bg-teal-50",
  },
  EXCITED: {
    label: "설레요",
    emoji: "🥰",
    weatherIcon: "🌈",
    color: "text-pink-500",
    bgColor: "bg-pink-50",
  },
  TIRED: {
    label: "피곤해요",
    emoji: "😴",
    weatherIcon: "🌙",
    color: "text-indigo-500",
    bgColor: "bg-indigo-50",
  },
}

// 스트릭 레벨
export const STREAK_LEVELS = [
  { days: 3, emoji: "🌱", label: "새싹" },
  { days: 7, emoji: "🌿", label: "풀잎" },
  { days: 14, emoji: "🌳", label: "나무" },
  { days: 30, emoji: "🌲", label: "숲" },
] as const

// 스트릭 레벨 가져오기
export function getStreakLevel(days: number) {
  for (let i = STREAK_LEVELS.length - 1; i >= 0; i--) {
    if (days >= STREAK_LEVELS[i].days) {
      return STREAK_LEVELS[i]
    }
  }
  return null
}
