"use client"

import { useState, useEffect } from "react"
import { AppShell } from "@/components/app-shell"
import { SectionHeader } from "@/components/section-header"
import { HintNotificationBanner } from "@/components/home/hint-notification-banner"
import { WeatherStatusCard } from "@/components/home/weather-status-card"
import { SlotPreviewCard } from "@/components/home/slot-preview-card"
import { PlannerReminderCard } from "@/components/home/planner-reminder-card"
import { getSlots, type SlotResponseDto } from "@/lib/api/slots"
import { getPlanEntries, formatDate, fromLocalTime, type PlanEntryResponse } from "@/lib/api/planner"
import { getMyProfile } from "@/lib/api/user"
import type { Slot } from "@/types/slot"

// API 응답 → Slot 타입 변환
function toSlot(dto: SlotResponseDto, index: number): Slot {
  return {
    id: dto.id,
    userId: dto.userId,
    slotNumber: index + 1,
    priority: dto.priority,
    currentMatchId: dto.currentMatchId,
    isQuickMatch: dto.isQuickMatch,
    status: dto.status,
    matchedUser: dto.matchedUser
      ? {
          id: dto.matchedUser.userId,
          nickname: dto.matchedUser.nickname,
          profileEmoji: dto.matchedUser.nickname.charAt(0),
        }
      : null,
  }
}

export default function HomePage() {
  const [slots, setSlots] = useState<Slot[]>([])
  const [userName, setUserName] = useState("")
  const [todayEntryCount, setTodayEntryCount] = useState(0)
  const [nextEvent, setNextEvent] = useState<{ title: string; time: string } | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function loadData() {
      try {
        // 병렬로 데이터 로드
        const [slotsData, profileData, planData] = await Promise.allSettled([
          getSlots(),
          getMyProfile(),
          getPlanEntries(formatDate(new Date())),
        ])

        // 슬롯
        if (slotsData.status === "fulfilled") {
          setSlots(slotsData.value.map(toSlot))
        }

        // 유저 이름
        if (profileData.status === "fulfilled") {
          setUserName(profileData.value.nickname)
        }

        // 오늘 일정
        if (planData.status === "fulfilled") {
          const entries = planData.value
          setTodayEntryCount(entries.length)

          // 현재 시간 이후의 가장 가까운 일정 찾기
          const now = new Date()
          const currentMinutes = now.getHours() * 60 + now.getMinutes()

          const upcoming = entries
            .filter((e: PlanEntryResponse) => {
              const startMinutes = e.startTime.hour * 60 + e.startTime.minute
              return startMinutes > currentMinutes
            })
            .sort((a: PlanEntryResponse, b: PlanEntryResponse) => {
              const aMin = a.startTime.hour * 60 + a.startTime.minute
              const bMin = b.startTime.hour * 60 + b.startTime.minute
              return aMin - bMin
            })

          if (upcoming.length > 0) {
            const next = upcoming[0]
            setNextEvent({
              title: next.name || "일정",
              time: `오늘 ${fromLocalTime(next.startTime)}`,
            })
          }
        }
      } catch (err) {
        console.error("홈 데이터 로드 실패:", err)
      } finally {
        setLoading(false)
      }
    }

    loadData()
  }, [])

  return (
    <AppShell noScroll>
      <div className="px-4 py-3 space-y-4">
        {/* Hint Question Notification Banner */}
        <HintNotificationBanner />

        {/* Greeting & Weather Card */}
        <WeatherStatusCard userName={userName || "사용자"} />

        {/* Slot Preview Section */}
        <section>
          <SectionHeader
            title="오늘의 예보"
            subtitle="함께할 사람을 찾고 있어요"
            action={{ label: "더보기" }}
            className="mb-2"
          />
          {loading ? (
            <p className="text-xs text-muted-foreground text-center py-4">로딩 중...</p>
          ) : slots.length > 0 ? (
            <div className="flex flex-col gap-2">
              {slots.map((slot) => (
                <SlotPreviewCard key={slot.id} slot={slot} />
              ))}
            </div>
          ) : (
            <div className="text-center py-6">
              <p className="text-xs text-muted-foreground">아직 슬롯이 없어요</p>
            </div>
          )}
        </section>

        {/* Planner Reminder Section */}
        <section>
          <SectionHeader
            title="다가오는 일정"
            action={{ label: "캘린더" }}
            className="mb-3"
          />
          <PlannerReminderCard
            upcomingCount={todayEntryCount}
            nextEvent={nextEvent || { title: "등록된 일정 없음", time: "오늘" }}
          />
        </section>

        <div className="h-4" />
      </div>
    </AppShell>
  )
}
