"use client"

import { AppShell } from "@/components/app-shell"
import { DiaryCalendar } from "@/components/diary/diary-calendar"
import type { DiaryEntry } from "@/types/diary"

// Sample data - today (2026-05-07) is not written yet
const sampleDiaries: DiaryEntry[] = [
  {
    id: "2",
    userId: "user-1",
    entryDate: "2026-05-06",
    content: "중간고사 준비로 하루종일 도서관에 있었다. 피곤했지만 뿌듯한 하루였다. 내일은 좀 쉬어야겠다.",
    emotionTag: "TIRED",
    streakCount: 4,
    createdAt: "2026-05-06T22:00:00Z",
    updatedAt: "2026-05-06T22:00:00Z",
  },
  {
    id: "3",
    userId: "user-1",
    entryDate: "2026-05-05",
    content: "날씨가 좋아서 캠퍼스를 산책했다. 벚꽃이 다 졌지만 초록초록한 나무들이 예뻤다. 평온한 하루.",
    emotionTag: "CALM",
    streakCount: 3,
    createdAt: "2026-05-05T20:15:00Z",
    updatedAt: "2026-05-05T20:15:00Z",
  },
  {
    id: "4",
    userId: "user-1",
    entryDate: "2026-05-04",
    content: "친구들이랑 맛집 탐방을 했다! 새로 생긴 파스타집이 정말 맛있었다. 오랜만에 신나는 주말이었다.",
    emotionTag: "HAPPY",
    streakCount: 2,
    createdAt: "2026-05-04T19:45:00Z",
    updatedAt: "2026-05-04T19:45:00Z",
  },
  {
    id: "5",
    userId: "user-1",
    entryDate: "2026-05-02",
    content: "과제 마감이 다가와서 불안했다. 하지만 열심히 해서 결국 제출했다!",
    emotionTag: "ANXIOUS",
    streakCount: 1,
    createdAt: "2026-05-02T23:50:00Z",
    updatedAt: "2026-05-02T23:50:00Z",
  },
  {
    id: "6",
    userId: "user-1",
    entryDate: "2026-04-30",
    content: "비오는 날 창밖을 보며 생각에 잠겼다. 조금 우울한 하루였지만 음악을 들으니 나아졌다.",
    emotionTag: "SAD",
    streakCount: 0,
    createdAt: "2026-04-30T21:00:00Z",
    updatedAt: "2026-04-30T21:00:00Z",
  },
]

export default function DiaryPage() {
  return (
    <AppShell title="일기">
      <div className="h-full flex flex-col p-4">
        <DiaryCalendar entries={sampleDiaries} className="flex-1" />
      </div>
    </AppShell>
  )
}
