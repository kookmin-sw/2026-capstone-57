import { AppShell } from "@/components/app-shell"
import { SectionHeader } from "@/components/section-header"
import { HintNotificationBanner } from "@/components/home/hint-notification-banner"
import { WeatherStatusCard } from "@/components/home/weather-status-card"
import { SlotPreviewCard } from "@/components/home/slot-preview-card"
import { PlannerReminderCard } from "@/components/home/planner-reminder-card"
import type { Slot } from "@/types/slot"

const sampleSlots: Slot[] = [
  {
    id: "1",
    userId: "user-1",
    slotNumber: 1,
    priority: "HOBBY",
    currentMatchId: "match-1",
    isQuickMatch: false,
    status: "ACTIVE",
    matchedUser: {
      id: "matched-user-1",
      nickname: "하늘빛",
      profileEmoji: "🌤️",
    },
    currentStage: "REVIEW",
    stageProgress: 90,
    daysRemaining: 1,
  },
  {
    id: "2",
    userId: "user-1",
    slotNumber: 2,
    priority: "INTEREST",
    currentMatchId: null,
    isQuickMatch: false,
    status: "EMPTY",
    matchedUser: null,
  },
  {
    id: "3",
    userId: "user-1",
    slotNumber: 3,
    priority: "IDEAL_TYPE",
    currentMatchId: "match-2",
    isQuickMatch: true,
    status: "COMPLETED",
    matchedUser: {
      id: "matched-user-2",
      nickname: "별빛",
      profileEmoji: "✨",
    },
  },
]

export default function HomePage() {
  return (
    <AppShell>
      <div className="px-4 py-3 space-y-4">
        {/* Hint Question Notification Banner */}
        <HintNotificationBanner />

        {/* Greeting & Weather Card */}
        <WeatherStatusCard userName="아리" />

        {/* Slot Preview Section - Main Focus */}
        <section>
          <SectionHeader 
            title="오늘의 예보" 
            subtitle="함께할 사람을 찾고 있어요"
            action={{ label: "더보기" }}
            className="mb-2"
          />
          <div className="flex flex-col gap-2.5">
            {sampleSlots.map((slot) => (
              <SlotPreviewCard key={slot.id} slot={slot} />
            ))}
          </div>
        </section>

        {/* Planner Reminder Section */}
        <section>
          <SectionHeader 
            title="다가오는 일정" 
            action={{ label: "캘린더" }}
            className="mb-3"
          />
          <PlannerReminderCard />
        </section>

        {/* Bottom Padding for nav */}
        <div className="h-4" />
      </div>
    </AppShell>
  )
}
