"use client"

import { useState, useEffect } from "react"
import { AppShell } from "@/components/app-shell"
import { DiaryCalendar } from "@/components/diary/diary-calendar"
import { getDiaryEntries, type DiaryEntryResponse } from "@/lib/api/diary"
import type { DiaryEntry } from "@/types/diary"

// API 응답 → DiaryEntry 변환
function toDiaryEntry(dto: DiaryEntryResponse): DiaryEntry {
  return {
    id: dto.id,
    userId: dto.userId,
    entryDate: dto.entryDate,
    content: dto.content,
    emotionTag: dto.emotionTag,
    streakCount: dto.streakCount,
    createdAt: dto.createdAt,
    updatedAt: dto.createdAt,
  }
}

export default function DiaryPage() {
  const [entries, setEntries] = useState<DiaryEntry[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function load() {
      try {
        const data = await getDiaryEntries({ page: 0, size: 100 })
        setEntries(data.content.map(toDiaryEntry))
      } catch (err) {
        console.error("일기 목록 조회 실패:", err)
        setEntries([])
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  return (
    <AppShell title="일기">
      <div className="h-full flex flex-col p-4">
        {loading ? (
          <div className="flex-1 flex items-center justify-center">
            <p className="text-sm text-muted-foreground">로딩 중...</p>
          </div>
        ) : (
          <DiaryCalendar entries={entries} className="flex-1" />
        )}
      </div>
    </AppShell>
  )
}
