"use client"

import { useParams, useRouter } from "next/navigation"
import { useMemo, useState } from "react"
import { ArrowLeft, Edit3, Sparkles } from "lucide-react"
import { AppShell } from "@/components/app-shell"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { DiaryAIChat } from "@/components/diary/diary-ai-chat"
import { DiaryCompiledView } from "@/components/diary/diary-compiled-view"
import { cn } from "@/lib/utils"
import type { DiaryEntry, EmotionTag } from "@/types/diary"
import { EMOTION_META } from "@/types/diary"
import type { ChatMessage } from "@/types/diary-chat"

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

function formatDisplayDate(dateStr: string) {
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const weekdays = ["일", "월", "화", "수", "목", "금", "토"]
  const weekday = weekdays[date.getDay()]
  return `${year}년 ${month}월 ${day}일 ${weekday}요일`
}

function isToday(dateStr: string) {
  const today = new Date()
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`
  return dateStr === todayStr
}

function isFutureDate(dateStr: string) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const checkDate = new Date(dateStr)
  checkDate.setHours(0, 0, 0, 0)
  return checkDate > today
}

type ViewState = "chat" | "compiled" | "view"

export default function DiaryDetailPage() {
  const params = useParams()
  const router = useRouter()
  const dateStr = params.date as string

  // Find diary entry for this date
  const entry = useMemo(() => {
    return sampleDiaries.find((d) => d.entryDate === dateStr)
  }, [dateStr])

  const isFuture = isFutureDate(dateStr)
  
  // State for AI chat flow
  const [viewState, setViewState] = useState<ViewState>(entry ? "view" : "chat")
  const [compiledContent, setCompiledContent] = useState<string>("")

  const handleChatComplete = (messages: ChatMessage[]) => {
    // Simulate AI compiling the diary from conversation
    const userMessages = messages.filter(m => m.role === "user").map(m => m.content)
    const compiled = userMessages.join("\n\n")
    setCompiledContent(compiled || "오늘은 특별한 일이 없었지만, 평온한 하루를 보냈다.")
    setViewState("compiled")
  }

  const handleSave = (content: string, emotion: EmotionTag) => {
    console.log("Saving diary:", { content, emotion, date: dateStr })
    router.push("/diary")
  }

  const handleBack = () => {
    router.push("/diary")
  }

  // Future date - should not be accessible but handle gracefully
  if (isFuture) {
    return (
      <AppShell 
        title="일기" 
        showBackButton 
        rightAction={
          <Button variant="ghost" size="icon" onClick={handleBack}>
            <ArrowLeft className="w-5 h-5" />
          </Button>
        }
      >
        <div className="p-4 flex flex-col items-center justify-center min-h-[60vh]">
          <div className="text-6xl mb-4">🔮</div>
          <h2 className="text-lg font-semibold text-foreground mb-2">
            아직 오지 않은 날이에요
          </h2>
          <p className="text-sm text-muted-foreground text-center mb-6">
            미래의 일기는 그 날이 되면 작성할 수 있어요
          </p>
          <Button onClick={handleBack}>
            돌아가기
          </Button>
        </div>
      </AppShell>
    )
  }

  // No entry exists - show AI chat or compiled view
  if (!entry) {
    // AI Chat view
    if (viewState === "chat") {
      return (
        <AppShell title="일기 쓰기">
          <div className="h-full flex flex-col">
            {/* Compact Back Button */}
            <div className="px-4 pt-3 pb-1">
              <button
                onClick={handleBack}
                className="flex items-center gap-1.5 text-muted-foreground hover:text-foreground transition-colors"
              >
                <ArrowLeft className="w-4 h-4" />
                <span className="text-xs">캘린더</span>
              </button>
            </div>
            
            {/* AI Chat */}
            <DiaryAIChat 
              date={new Date(dateStr)} 
              onComplete={handleChatComplete}
              className="flex-1"
            />
          </div>
        </AppShell>
      )
    }

    // Compiled diary view
    if (viewState === "compiled") {
      return (
        <AppShell title="일기 완성">
          <DiaryCompiledView
            content={compiledContent}
            suggestedEmotion="CALM"
            summary="하루를 돌아보며 성장한 나"
            date={new Date(dateStr)}
            onSave={handleSave}
            onEdit={() => setViewState("chat")}
          />
        </AppShell>
      )
    }
  }

  // Entry exists - show detail view
  const emotionMeta = EMOTION_META[entry.emotionTag]

  return (
    <AppShell title="일기">
      <div className="p-4 space-y-4">
        {/* Back Button */}
        <button
          onClick={handleBack}
          className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span className="text-sm">캘린더로 돌아가기</span>
        </button>

        {/* Date Header with Emotion */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-2xl">{emotionMeta.weatherIcon}</span>
            <div>
              <h2 className="text-lg font-semibold text-foreground">
                {formatDisplayDate(entry.entryDate)}
              </h2>
              <p className="text-sm text-muted-foreground">
                {emotionMeta.label}
              </p>
            </div>
          </div>
          <Button variant="ghost" size="icon" className="rounded-full">
            <Edit3 className="w-4 h-4" />
          </Button>
        </div>

        {/* Diary Content Card */}
        <Card className={cn(
          "border-0 shadow-md overflow-hidden",
          emotionMeta.bgColor
        )}>
          <CardContent className="p-0">
            {/* Paper texture lines */}
            <div 
              className="p-6 min-h-[300px]"
              style={{
                backgroundImage: "repeating-linear-gradient(transparent, transparent 27px, rgba(0,0,0,0.05) 28px)",
                backgroundSize: "100% 28px",
              }}
            >
              <p className="text-foreground/90 leading-7 whitespace-pre-wrap">
                {entry.content}
              </p>
            </div>
          </CardContent>
        </Card>

        {/* Streak Info */}
        {entry.streakCount > 0 && (
          <div className="flex items-center justify-center gap-2 py-3">
            <Sparkles className="w-4 h-4 text-amber-500" />
            <span className="text-sm text-muted-foreground">
              이 날까지 <span className="font-semibold text-amber-600">{entry.streakCount}일</span> 연속 작성 중이었어요
            </span>
          </div>
        )}

        {/* Created Time */}
        <p className="text-xs text-muted-foreground text-center">
          작성: {new Date(entry.createdAt).toLocaleString("ko-KR", {
            year: "numeric",
            month: "long",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
          })}
        </p>
      </div>
    </AppShell>
  )
}
