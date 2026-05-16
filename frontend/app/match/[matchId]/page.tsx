"use client"

import { useState, useEffect, useRef } from "react"
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
  createChatSession,
  getChatSession,
  getChatMessages,
  sendHint,
  getHints,
  getMission,
  confirmMission,
  type InteractionStateDto,
  type ChatSessionDto,
  type HintQuestionDto,
} from "@/lib/api/interaction"
import {
  connectChatSocket,
  sendChatMessage,
  disconnectChatSocket,
  type IncomingChatMessage,
} from "@/lib/chat-socket"
import { getSlots } from "@/lib/api/slots"
import type { InteractionStage } from "@/types/slot"
import type { QuizQuestion, ChatMessage, MissionInfo, MatchPartner } from "@/types/match"

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

  // Quiz state
  const [quizQuestions, setQuizQuestions] = useState<QuizQuestion[]>([])
  const [quizHints, setQuizHints] = useState<HintQuestionDto[]>([])
  const [quizWaiting, setQuizWaiting] = useState(false)

  // Chat state
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([])
  const [chatSession, setChatSession] = useState<ChatSessionDto | null>(null)
  const [usedTokens, setUsedTokens] = useState(0)
  const [chatEnded, setChatEnded] = useState(false)
  const stompConnectedRef = useRef(false)

  // Mission state
  const [mission, setMission] = useState<MissionInfo | null>(null)

  // 초기 데이터 로드
  useEffect(() => {
    async function load() {
      try {
        const state = await getInteractionState(matchId)
        setInteraction(state)

        const currentStage = STAGE_MAP[state.currentStage] || "QUIZ"
        setActiveStage(currentStage)

        // 이미 퀴즈 완료 후 대기 중인 경우 감지
        const userId = localStorage.getItem("userId") || ""
        const quizCompletedBy: string[] = state.stageData?.quizCompletedBy || []
        if (state.currentStage === 1 && state.stageStatus === "WAITING" && quizCompletedBy.includes(userId)) {
          setQuizWaiting(true)
        }

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

    return () => {
      disconnectChatSocket()
    }
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
              correctIndex: -1,
            }))
          )
          try {
            const hints = await getHints(matchId)
            setQuizHints(hints)
          } catch {
            setQuizHints([])
          }
          break
        }
        case "CHAT": {
          try {
            const session = await getChatSession(matchId)
            setChatSession(session)
            setUsedTokens(session.usedTokens)
            if (session.status === "ENDED") {
              setChatEnded(true)
            }
            // 기존 메시지 로드
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
            // WebSocket 연결
            if (session.status === "ACTIVE") {
              initChatSocket(session.sessionId)
            }
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

  /** WebSocket(STOMP) 연결 초기화 */
  const initChatSocket = (sessionId: string) => {
    if (stompConnectedRef.current) return
    stompConnectedRef.current = true

    const userId = localStorage.getItem("userId") || ""

    connectChatSocket(sessionId, {
      onMessage: (msg: IncomingChatMessage) => {
        const chatMsg: ChatMessage = {
          id: msg.messageId,
          senderId: msg.senderId,
          content: msg.content,
          timestamp: new Date(msg.createdAt).toLocaleTimeString("ko-KR", {
            hour: "2-digit",
            minute: "2-digit",
          }),
          isMe: msg.senderId === userId,
        }
        setChatMessages((prev) => [...prev, chatMsg])
      },
      onTokenUpdate: (tokens: number) => {
        setUsedTokens(tokens)
      },
      onSessionEnd: () => {
        setChatEnded(true)
        disconnectChatSocket()
        stompConnectedRef.current = false
      },
      onError: (err) => {
        console.error("채팅 소켓 에러:", err)
      },
    })
  }

  /** 퀴즈 답안 제출 핸들러 — allCompleted 시 채팅 세션 생성 */
  const handleSubmitAnswer = async (quizIndex: number, answer: number) => {
    try {
      const result = await submitQuiz(matchId, {
        quizIndex: quizIndex + 1, // 1-based
        answer,
      })

      if (result.allCompleted) {
        // 양쪽 모두 퀴즈 완료 → 채팅 세션 생성
        await transitionToChat()
        return { correctAnswer: result.correctAnswer, isCorrect: result.isCorrect }
      }

      return { correctAnswer: result.correctAnswer, isCorrect: result.isCorrect }
    } catch (err) {
      console.error("퀴즈 제출 실패:", err)
      return null
    }
  }

  /** 채팅 단계로 전환: 세션 생성 → WebSocket 연결 */
  const transitionToChat = async () => {
    try {
      // 1. 채팅 세션 생성
      const session = await createChatSession(matchId)
      setChatSession(session)
      setUsedTokens(session.usedTokens)
      setChatEnded(false)
      setChatMessages([])

      // 2. 단계 전환
      setActiveStage("CHAT")
      setQuizWaiting(false)

      // 3. 상호작용 상태 갱신
      const state = await getInteractionState(matchId)
      setInteraction(state)

      // 4. WebSocket 연결
      initChatSocket(session.sessionId)
    } catch (err) {
      console.error("채팅 세션 생성 실패:", err)
    }
  }

  /** 퀴즈 완료 핸들러 (마지막 문항 "완료" 버튼) */
  const handleQuizComplete = async () => {
    // allCompleted가 이미 submitAnswer에서 처리되었을 수 있음
    // 여기서는 대기 상태로 전환 (상대방이 아직 완료하지 않은 경우)
    setQuizWaiting(true)
  }

  // 대기 중일 때 폴링으로 상태 확인
  useEffect(() => {
    if (!quizWaiting) return

    const interval = setInterval(async () => {
      try {
        const state = await getInteractionState(matchId)
        setInteraction(state)

        if (state.currentStage >= 2 && state.stageStatus !== "WAITING") {
          setQuizWaiting(false)
          await transitionToChat()
        }
      } catch (err) {
        console.error("상태 폴링 실패:", err)
      }
    }, 5000)

    return () => clearInterval(interval)
  }, [quizWaiting, matchId])

  /** 채팅 메시지 전송 */
  const handleSendMessage = (content: string) => {
    if (!chatSession) return
    sendChatMessage(chatSession.sessionId, content)
  }

  const handleBack = () => {
    router.push("/")
  }

  const handleStageSelect = async (stage: InteractionStage) => {
    setActiveStage(stage)
    await loadStageData(stage)
  }

  const handleGameSelect = (gameId: string) => {
    console.log("게임 시작:", gameId)
  }

  const handleMissionComplete = async () => {
    try {
      await confirmMission(matchId)
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
        if (quizWaiting) {
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
        return (
          <QuizStage
            questions={quizQuestions.length > 0 ? quizQuestions : [{ id: "loading", question: "로딩 중...", options: [], correctIndex: -1 }]}
            hints={quizHints.map((h) => ({ id: h.id, question: h.question, answer: h.answer, status: h.status, quizIndex: h.quizIndex, senderId: h.senderId }))}
            currentUserId={localStorage.getItem("userId") || ""}
            matchId={matchId}
            onComplete={handleQuizComplete}
            onSubmitAnswer={handleSubmitAnswer}
            onHintsUpdate={(updated) => setQuizHints((prev) =>
              prev.map((h) => {
                const match = updated.find((u) => u.id === h.id)
                return match ? { ...h, answer: match.answer, status: match.status } : h
              })
            )}
            onSendHint={async (question, quizIndex) => {
              try {
                const newHint = await sendHint(matchId, question, quizIndex)
                setQuizHints((prev) => [...prev, newHint])
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
            tokenLimit={chatSession?.tokenLimit || 0}
            usedTokens={usedTokens}
            icebreakerQuestion={chatSession?.icebreakerQuestion}
            isEnded={chatEnded}
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
      <div className="px-4 py-2 h-full flex flex-col">
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
        <div className="flex-1 mt-2 min-h-0 overflow-y-auto">
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
