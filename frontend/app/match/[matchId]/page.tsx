"use client"

import { useState, useEffect } from "react"
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
import {
  getInteractionState,
  getQuiz,
  submitQuiz,
  completeQuiz,
  getMission,
  confirmMission,
  getChatSession,
  getChatMessages,
  sendHint,
  type InteractionStateDto,
  type QuizQuestionResponse,
  type MissionDto,
  type ChatMessageDto,
} from "@/lib/api/interaction"
import { getSlots } from "@/lib/api/slots"
import type { InteractionStage } from "@/types/slot"
import type { QuizQuestion, ChatMessage, GameOption, MissionInfo, MatchPartner } from "@/types/match"

// 서버 stage 번호 → InteractionStage 매핑
const STAGE_MAP: Record<number, InteractionStage> = {
  1: "QUIZ",
  2: "CHAT",
  3: "GAME",
  4: "MISSION",
  5: "REVIEW",
}

function getCompletedStages(currentStage: number): InteractionStage[] {
  const stages: InteractionStage[] = []
  for (let i = 1; i < currentStage; i++) {
    if (STAGE_MAP[i]) stages.push(STAGE_MAP[i])
  }
  return stages
}

export default function MatchDetailPage() {
  const params = useParams()
  const router = useRouter()
  const matchId = params.matchId as string

  const [loading, setLoading] = useState(true)
  const [interaction, setInteraction] = useState<InteractionStateDto | null>(null)
  const [partner, setPartner] = useState<MatchPartner | null>(null)
  const [activeStage, setActiveStage] = useState<InteractionStage>("QUIZ")

  // Stage data
  const [quizQuestions, setQuizQuestions] = useState<QuizQuestion[]>([])
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([])
  const [chatSessionId, setChatSessionId] = useState<string | null>(null)
  const [mission, setMission] = useState<MissionInfo | null>(null)

  // 초기 데이터 로드
  useEffect(() => {
    async function load() {
      try {
        // 상호작용 상태 조회
        const state = await getInteractionState(matchId)
        setInteraction(state)

        const currentStage = STAGE_MAP[state.currentStage] || "QUIZ"
        setActiveStage(currentStage)

        // 슬롯에서 매칭 상대 정보 가져오기
        try {
          const slots = await getSlots()
          const matchedSlot = slots.find((s) => s.currentMatchId === matchId)
          if (matchedSlot?.matchedUser) {
            setPartner({
              id: matchedSlot.matchedUser.userId,
              nickname: matchedSlot.matchedUser.nickname,
              profileEmoji: matchedSlot.matchedUser.nickname.charAt(0),
              department: "",
              studentYear: "",
              hobbies: [],
            })
          }
        } catch {}

        // 현재 단계에 맞는 데이터 로드
        await loadStageData(currentStage)
      } catch (err) {
        console.error("매칭 데이터 로드 실패:", err)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [matchId])

  const loadStageData = async (stage: InteractionStage) => {
    try {
      switch (stage) {
        case "QUIZ": {
          const questions = await getQuiz(matchId)
          setQuizQuestions(
            questions.map((q) => ({
              id: `q${q.quizIndex}`,
              question: q.question,
              options: q.options,
              correctIndex: -1, // 서버가 정답을 안 줌
            }))
          )
          break
        }
        case "CHAT": {
          try {
            const session = await getChatSession(matchId)
            setChatSessionId(session.sessionId)
            const messages = await getChatMessages(session.sessionId)
            const userId = localStorage.getItem("userId") || ""
            setChatMessages(
              messages.map((m) => ({
                id: m.messageId,
                senderId: m.senderId,
                content: m.content,
                timestamp: new Date(m.createdAt).toLocaleTimeString("ko-KR", {
                  hour: "2-digit",
                  minute: "2-digit",
                }),
                isMe: m.senderId === userId,
              }))
            )
          } catch {
            setChatMessages([])
          }
          break
        }
        case "MISSION": {
          try {
            const m = await getMission(matchId)
            const deadline = new Date(m.deadline)
            const now = new Date()
            const daysLeft = Math.max(0, Math.ceil((deadline.getTime() - now.getTime()) / (1000 * 60 * 60 * 24)))
            setMission({
              location: m.location,
              locationDetail: m.description || m.location,
              recommendedTime: m.activity,
              deadline: m.deadline,
              daysLeft,
            })
          } catch {
            setMission(null)
          }
          break
        }
      }
    } catch (err) {
      console.error(`${stage} 데이터 로드 실패:`, err)
    }
  }

  const handleBack = () => {
    router.push("/")
  }

  const handleStageSelect = async (stage: InteractionStage) => {
    setActiveStage(stage)
    await loadStageData(stage)
  }

  const handleQuizComplete = async () => {
    try {
      const result = await completeQuiz(matchId)
      setInteraction(result)
      const nextStage = STAGE_MAP[result.currentStage] || "CHAT"
      setActiveStage(nextStage)
      await loadStageData(nextStage)
    } catch (err) {
      console.error("퀴즈 완료 실패:", err)
    }
  }

  const handleSendMessage = (content: string) => {
    // 채팅은 WebSocket 기반이므로 여기서는 UI만 업데이트
    const newMessage: ChatMessage = {
      id: `m${Date.now()}`,
      senderId: "me",
      content,
      timestamp: new Date().toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" }),
      isMe: true,
    }
    setChatMessages((prev) => [...prev, newMessage])
  }

  const handleGameSelect = (gameId: string) => {
    console.log("게임 시작:", gameId)
  }

  const handleMissionComplete = async () => {
    try {
      await confirmMission(matchId)
      // 상태 새로고침
      const state = await getInteractionState(matchId)
      setInteraction(state)
      const nextStage = STAGE_MAP[state.currentStage] || "REVIEW"
      setActiveStage(nextStage)
    } catch (err) {
      console.error("미션 완료 실패:", err)
    }
  }

  const handleReviewSave = (content: string, rating: number, mode: "ai" | "free") => {
    console.log("회고 저장:", { content, rating, mode })
    router.push("/")
  }

  const currentStageNum = interaction?.currentStage || 1
  const currentStage = STAGE_MAP[currentStageNum] || "QUIZ"
  const completedStages = getCompletedStages(currentStageNum)

  const renderStageContent = () => {
    switch (activeStage) {
      case "QUIZ":
        return (
          <QuizStage
            questions={quizQuestions.length > 0 ? quizQuestions : [{ id: "loading", question: "로딩 중...", options: [], correctIndex: -1 }]}
            onComplete={handleQuizComplete}
            onSubmitAnswer={async (quizIndex, answer) => {
              try {
                const result = await submitQuiz(matchId, {
                  quizIndex: quizIndex + 1,  // 1-based로 변환
                  answer                      // 0-based 그대로 유지
                })
                return { correctAnswer: result.correctAnswer, isCorrect: result.isCorrect }
              } catch (err) {
                console.error("퀴즈 제출 실패:", err)
                return null
              }
            }}
            onSendHint={async (question) => {
              try {
                await sendHint(matchId, question)
              } catch (err) {
                console.error("힌트 전송 실패:", err)
              }
            }}
          />
        )
      case "CHAT":
        return (
          <ChatStage
            messages={chatMessages}
            remainingTime="--:--"
            onSendMessage={handleSendMessage}
          />
        )
      case "GAME":
        return (
          <GameStage
            games={[{ id: "g1", name: "달빛찾기", description: "함께 달빛을 찾아서 탈출하세요", icon: "🌙" }]}
            onSelectGame={handleGameSelect}
          />
        )
      case "MISSION":
        return mission ? (
          <MissionStage
            mission={mission}
            onExtend={() => {}}
            onComplete={handleMissionComplete}
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
      <div className="px-4 py-2 h-full flex flex-col overflow-hidden">
        {/* Back */}
        <button
          onClick={handleBack}
          className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors shrink-0 mb-2"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>홈으로</span>
        </button>

        {/* Hero card */}
        <div className="shrink-0">
          <MatchHeroCard
            partner={partner || { id: "", nickname: "상대방", profileEmoji: "?", department: "", studentYear: "", hobbies: [] }}
            intimacy={0}
          />
        </div>

        {/* Stage stepper */}
        <div className="shrink-0 mt-2">
          <StageStepper
            currentStage={currentStage}
            completedStages={completedStages}
            activeStage={activeStage}
            onStageSelect={handleStageSelect}
          />
        </div>

        {/* Stage content */}
        <div className="flex-1 mt-2 min-h-0">
          {renderStageContent()}
        </div>

        {/* Report */}
        <button className="shrink-0 w-full flex items-center justify-center gap-1 py-1.5 text-muted-foreground text-[10px] hover:text-foreground transition-colors mt-1">
          <Flag className="w-2.5 h-2.5" />
          <span>신고하기</span>
        </button>
      </div>
    </AppShell>
  )
}
