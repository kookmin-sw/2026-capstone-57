"use client"

import { useState } from "react"
import { useParams, useRouter } from "next/navigation"
import { ArrowLeft, Flag } from "lucide-react"
import { AppShell } from "@/components/app-shell"
import { MatchHeroCard } from "@/components/match/match-hero-card"
import { StageStepper } from "@/components/match/stage-stepper"
import { QuizStage } from "@/components/match/stages/quiz-stage"
import { ChatStage } from "@/components/match/stages/chat-stage"
import { GameStage } from "@/components/match/stages/game-stage"
import { MissionStage } from "@/components/match/stages/mission-stage"
import { ReviewStage } from "@/components/match/stages/review-stage"
import type { Match, QuizQuestion, ChatMessage, GameOption, MissionInfo } from "@/types/match"
import type { InteractionStage } from "@/types/slot"

// Sample data
const sampleMatch: Match = {
  id: "match-1",
  partnerId: "partner-1",
  partner: {
    id: "partner-1",
    nickname: "하늘빛",
    profileEmoji: "🌤️",
    department: "경영학과",
    studentYear: "22학번",
    hobbies: ["독서", "카페투어", "러닝"],
  },
  currentStage: "REVIEW",
  completedStages: ["QUIZ", "CHAT", "GAME", "MISSION"],
  intimacy: 85,
  createdAt: "2026-05-05T10:00:00Z",
}

const sampleQuizzes: QuizQuestion[] = [
  {
    id: "q1",
    question: "상대방이 좋아하는 계절은?",
    options: ["봄", "여름", "가을", "겨울"],
    correctIndex: 2,
  },
  {
    id: "q2",
    question: "상대방의 MBTI 첫 글자는?",
    options: ["E", "I"],
    correctIndex: 1,
  },
  {
    id: "q3",
    question: "상대방이 선호하는 음료는?",
    options: ["커피", "차", "주스", "탄산음료"],
    correctIndex: 0,
  },
]

const sampleMessages: ChatMessage[] = [
  { id: "m1", senderId: "partner-1", content: "안녕하세요! 반가워요 :)", timestamp: "14:30", isMe: false },
  { id: "m2", senderId: "me", content: "안녕하세요! 저도 반가워요!", timestamp: "14:31", isMe: true },
  { id: "m3", senderId: "partner-1", content: "퀴즈 재밌었어요. 제 취미가 독서라서 맞추기 어려웠을 것 같은데", timestamp: "14:32", isMe: false },
]

const sampleGames: GameOption[] = [
  { id: "g1", name: "이심전심 그림 맞추기", description: "같은 주제로 그림을 그리고 비교해요", icon: "🎨" },
  { id: "g2", name: "별자리 퍼즐", description: "협동해서 별자리를 완성해요", icon: "🧩" },
  { id: "g3", name: "이야기 이어쓰기", description: "번갈아 가며 짧은 이야기를 만들어요", icon: "📖" },
]

const sampleMission: MissionInfo = {
  location: "학생회관 카페",
  locationDetail: "학생회관 1F · 노을카페",
  recommendedTime: "내일 14:30",
  deadline: "2026-05-08",
  daysLeft: 1,
}

export default function MatchDetailPage() {
  const params = useParams()
  const router = useRouter()
  const matchId = params.matchId as string

  const [match, setMatch] = useState<Match>(sampleMatch)
  const [activeStage, setActiveStage] = useState<InteractionStage>(match.currentStage)
  const [messages, setMessages] = useState<ChatMessage[]>(sampleMessages)

  const handleBack = () => {
    router.push("/")
  }

  const handleStageSelect = (stage: InteractionStage) => {
    setActiveStage(stage)
  }

  const handleQuizComplete = () => {
    setMatch((prev) => ({
      ...prev,
      completedStages: [...prev.completedStages, "QUIZ"],
      currentStage: "CHAT",
      intimacy: prev.intimacy + 15,
    }))
    setActiveStage("CHAT")
  }

  const handleSendMessage = (content: string) => {
    const newMessage: ChatMessage = {
      id: `m${messages.length + 1}`,
      senderId: "me",
      content,
      timestamp: new Date().toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" }),
      isMe: true,
    }
    setMessages((prev) => [...prev, newMessage])
  }

  const handleGameSelect = (gameId: string) => {
    console.log("Selected game:", gameId)
    // TODO: Start game
  }

  const handleMissionExtend = () => {
    console.log("Mission extended")
    // TODO: Extend mission deadline
  }

  const handleMissionComplete = () => {
    setMatch((prev) => ({
      ...prev,
      completedStages: [...prev.completedStages, "MISSION"],
      currentStage: "REVIEW",
      intimacy: prev.intimacy + 20,
    }))
    setActiveStage("REVIEW")
  }

  const handleReviewSave = (content: string, rating: number, mode: "ai" | "free") => {
    console.log("Review saved:", { content, rating, mode })
    setMatch((prev) => ({
      ...prev,
      completedStages: [...prev.completedStages, "REVIEW"],
      intimacy: 100,
    }))
    router.push("/")
  }

  const renderStageContent = () => {
    switch (activeStage) {
      case "QUIZ":
        return <QuizStage questions={sampleQuizzes} onComplete={handleQuizComplete} />
      case "CHAT":
        return (
          <ChatStage 
            messages={messages} 
            remainingTime="23:45" 
            onSendMessage={handleSendMessage} 
          />
        )
      case "GAME":
        return <GameStage games={sampleGames} onSelectGame={handleGameSelect} />
      case "MISSION":
        return (
          <MissionStage 
            mission={sampleMission} 
            onExtend={handleMissionExtend} 
            onComplete={handleMissionComplete} 
          />
        )
      case "REVIEW":
        return <ReviewStage onSave={handleReviewSave} />
      default:
        return null
    }
  }

  return (
    <AppShell>
      <div className="px-5 py-4 pb-24 space-y-6">
        {/* Back navigation */}
        <button
          onClick={handleBack}
          className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>홈으로</span>
        </button>

        {/* Hero card */}
        <MatchHeroCard partner={match.partner} intimacy={match.intimacy} />

        {/* Stage stepper */}
        <StageStepper
          currentStage={match.currentStage}
          completedStages={match.completedStages}
          activeStage={activeStage}
          onStageSelect={handleStageSelect}
        />

        {/* Stage content */}
        <div className="mt-6">
          {renderStageContent()}
        </div>

        {/* Report button */}
        <button className="w-full flex items-center justify-center gap-2 py-3 rounded-2xl border border-border/50 bg-card/40 text-muted-foreground text-xs hover:bg-card/60 transition-colors">
          <Flag className="w-3.5 h-3.5" />
          <span>신고하기</span>
        </button>
      </div>
    </AppShell>
  )
}
