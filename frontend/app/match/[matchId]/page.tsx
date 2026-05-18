"use client"

import { useCallback, useEffect, useRef } from "react"
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
import { sendHint } from "@/lib/api/interaction"
import { useMatchState } from "@/hooks/match/use-match-state"
import { useQuizStage } from "@/hooks/match/use-quiz-stage"
import { useChatStage } from "@/hooks/match/use-chat-stage"
import { useGameStage } from "@/hooks/match/use-game-stage"
import { useMissionStage } from "@/hooks/match/use-mission-stage"
import type { InteractionStage } from "@/types/slot"

export default function MatchDetailPage() {
  const params = useParams()
  const router = useRouter()
  const matchId = params.matchId as string

  // Shared match state
  const {
    loading,
    interaction,
    setInteraction,
    partner,
    activeStage,
    setActiveStage,
    currentStage,
    completedStages,
  } = useMatchState(matchId)

  // Chat stage hook
  const chat = useChatStage({
    matchId,
    setInteraction,
    setActiveStage,
  })

  // Quiz stage hook
  const quiz = useQuizStage({
    matchId,
    interaction,
    setInteraction,
    setActiveStage,
    onChatSessionCreated: chat.setupFromSession,
  })

  // Mission stage hook
  const mission = useMissionStage({
    matchId,
    setInteraction,
    setActiveStage,
  })

  // Stage data loader ref (to avoid circular dependency with game hook)
  const loadStageDataRef = useRef<(stage: InteractionStage) => Promise<void>>(undefined)

  // Game stage hook
  const game = useGameStage({
    matchId,
    setInteraction,
    setActiveStage,
    loadStageData: async (stage: InteractionStage) => {
      if (loadStageDataRef.current) {
        await loadStageDataRef.current(stage)
      }
    },
  })

  // Stage data loader
  const loadStageData = useCallback(async (stage: InteractionStage) => {
    switch (stage) {
      case "QUIZ":
        await quiz.loadQuizData()
        break
      case "CHAT":
        await chat.loadChatData()
        break
      case "GAME":
        await game.loadGameData()
        break
      case "MISSION":
        await mission.loadMissionData()
        break
    }
  }, [quiz.loadQuizData, chat.loadChatData, game.loadGameData, mission.loadMissionData])

  // Keep ref in sync
  loadStageDataRef.current = loadStageData

  // Load initial stage data after mount
  const hasLoadedInitialStage = useRef(false)
  useEffect(() => {
    if (!loading && interaction && !hasLoadedInitialStage.current) {
      hasLoadedInitialStage.current = true
      loadStageData(activeStage)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loading])

  const handleBack = () => router.push("/")

  const handleStageSelect = async (stage: InteractionStage) => {
    setActiveStage(stage)
    await loadStageData(stage)
  }

  const handleReviewSave = (content: string, rating: number, mode: "ai" | "free") => {
    console.log("회고 저장:", { content, rating, mode })
    router.push("/")
  }

  const renderStageContent = () => {
    switch (activeStage) {
      case "QUIZ":
        if (quiz.quizWaiting) {
          return (
            <div className="flex flex-col items-center justify-center bg-card rounded-2xl p-6 shadow-sm border border-border/30 text-center gap-3">
              <span className="text-3xl">⏳</span>
              <p className="text-sm font-semibold text-foreground">퀴즈를 모두 풀었어요!</p>
              <p className="text-xs text-muted-foreground">
                상대방이 퀴즈를 완료하면<br />다음 단계로 넘어갑니다
              </p>
            </div>
          )
        }
        if (quiz.quizGenerating) {
          return (
            <div className="flex flex-col items-center justify-center bg-card rounded-2xl p-6 shadow-sm border border-border/30 text-center gap-4">
              <div className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-primary animate-bounce [animation-delay:0ms]" />
                <span className="w-2 h-2 rounded-full bg-primary animate-bounce [animation-delay:150ms]" />
                <span className="w-2 h-2 rounded-full bg-primary animate-bounce [animation-delay:300ms]" />
              </div>
              <p className="text-sm font-semibold text-foreground">퀴즈를 준비하고 있어요</p>
              <p className="text-xs text-muted-foreground">
                잠시 뒤 다시 들어와주세요
              </p>
            </div>
          )
        }
        return (
          <QuizStage
            questions={quiz.quizQuestions.length > 0 ? quiz.quizQuestions : [{ id: "loading", question: "로딩 중...", options: [], correctIndex: -1 }]}
            hints={quiz.quizHints.map((h) => ({ id: h.id, question: h.question, answer: h.answer, status: h.status, quizIndex: h.quizIndex, senderId: h.senderId }))}
            currentUserId={typeof window !== "undefined" ? localStorage.getItem("userId") || "" : ""}
            matchId={matchId}
            onComplete={quiz.handleQuizComplete}
            onSubmitAnswer={quiz.handleSubmitAnswer}
            onHintsUpdate={(updated) => quiz.setQuizHints((prev) =>
              prev.map((h) => {
                const match = updated.find((u) => u.id === h.id)
                return match ? { ...h, answer: match.answer, status: match.status } : h
              })
            )}
            onSendHint={async (question, quizIndex) => {
              try {
                const newHint = await sendHint(matchId, question, quizIndex)
                quiz.setQuizHints((prev) => [...prev, newHint])
              } catch (err) {
                console.error("힌트 전송 실패:", err)
              }
            }}
          />
        )
      case "CHAT":
        return (
          <ChatStage
            messages={chat.chatMessages}
            tokenLimit={chat.chatSession?.tokenLimit || 0}
            usedTokens={chat.usedTokens}
            icebreakerQuestion={chat.chatSession?.icebreakerQuestion}
            isEnded={chat.chatEnded}
            isConnected={chat.isStompConnected}
            onSendMessage={chat.handleSendMessage}
          />
        )
      case "GAME":
        return (
          <GameStage
            games={[{ id: "g1", name: "달빛찾기", description: "함께 달빛을 찾아서 탈출하세요", icon: "🌙" }]}
            isWaiting={game.gameWaiting}
            onSelectGame={game.handleGameSelect}
          />
        )
      case "MISSION":
        return mission.mission ? (
          <MissionStage
            mission={mission.mission}
            onExtend={() => {}}
            onComplete={mission.handleMissionComplete}
          />
        ) : (
          <div className="h-full flex items-center justify-center">
            <p className="text-sm text-muted-foreground">미션이 아직 생성되지 않았어요</p>
          </div>
        )
      case "REVIEW":
        return <ReviewStage onSave={handleReviewSave} />
      default:
        return null
    }
  }

  if (loading) {
    return (
      <AppShell noScroll>
        <div className="flex items-center justify-center h-full">
          <p className="text-sm text-muted-foreground">로딩 중...</p>
        </div>
      </AppShell>
    )
  }

  return (
    <AppShell noScroll>
      <div className="px-4 py-2 h-full flex flex-col">
        <button
          onClick={handleBack}
          className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors shrink-0 mb-2"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>홈으로</span>
        </button>

        <div className="shrink-0">
          <MatchHeroCard
            partner={partner || { id: "", nickname: "상대방", profileEmoji: "?", department: "", studentYear: "", hobbies: [] }}
            intimacy={0}
          />
        </div>

        <div className="shrink-0 mt-2">
          <StageStepper
            currentStage={currentStage}
            completedStages={completedStages}
            activeStage={activeStage}
            onStageSelect={handleStageSelect}
          />
        </div>

        <div className="flex-1 mt-2 min-h-0 overflow-y-auto">
          {renderStageContent()}
        </div>

        <button className="shrink-0 w-full flex items-center justify-center gap-1 py-1.5 text-muted-foreground text-[10px] hover:text-foreground transition-colors mt-1">
          <Flag className="w-2.5 h-2.5" />
          <span>신고하기</span>
        </button>
      </div>
    </AppShell>
  )
}
