"use client"

import { useParams, useRouter } from "next/navigation"
import { useMemo, useState, useEffect, useRef } from "react"
import { ArrowLeft, Edit3, Sparkles } from "lucide-react"
import { AppShell } from "@/components/app-shell"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import type { DiaryEntry, EmotionTag } from "@/types/diary"
import { EMOTION_META } from "@/types/diary"
import {
  getDiaryEntries,
  createDiary,
  startDiarySession,
  getActiveSession,
  answerDiaryQuestion,
  generateDiary,
  confirmDiary,
  cancelDiarySession,
  type DiaryEntryResponse,
  type DiarySessionResponse,
  type GeneratedDiaryPreview,
} from "@/lib/api/diary"

function formatDisplayDate(dateStr: string) {
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const weekdays = ["일", "월", "화", "수", "목", "금", "토"]
  const weekday = weekdays[date.getDay()]
  return `${year}년 ${month}월 ${day}일 ${weekday}요일`
}

function isFutureDate(dateStr: string) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const checkDate = new Date(dateStr)
  checkDate.setHours(0, 0, 0, 0)
  return checkDate > today
}

type ViewState = "loading" | "chat" | "compiled" | "view" | "future"

export default function DiaryDetailPage() {
  const params = useParams()
  const router = useRouter()
  const dateStr = params.date as string

  const [viewState, setViewState] = useState<ViewState>("loading")
  const [entry, setEntry] = useState<DiaryEntry | null>(null)

  // AI session state
  const [session, setSession] = useState<DiarySessionResponse | null>(null)
  const [currentQuestion, setCurrentQuestion] = useState("")
  const [answerInput, setAnswerInput] = useState("")
  const [chatHistory, setChatHistory] = useState<{ question: string; answer: string }[]>([])
  const [aiLoading, setAiLoading] = useState(false)

  // AI generating state
  const [generating, setGenerating] = useState(false)

  // Compiled state
  const [compiledContent, setCompiledContent] = useState("")
  const [suggestedEmotion, setSuggestedEmotion] = useState<EmotionTag>("CALM")

  // Auto scroll ref
  const chatEndRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  // Manual write state
  const [manualContent, setManualContent] = useState("")
  const [manualEmotion, setManualEmotion] = useState<EmotionTag>("CALM")

  // Auto scroll to bottom when chat updates
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [chatHistory, currentQuestion, aiLoading])

  // Keep focus on input after AI responds
  useEffect(() => {
    if (!aiLoading && !generating && session) {
      inputRef.current?.focus()
    }
  }, [aiLoading, generating])

  useEffect(() => {
    if (isFutureDate(dateStr)) {
      setViewState("future")
      return
    }
    loadEntry()
  }, [dateStr])

  const loadEntry = async () => {
    try {
      const data = await getDiaryEntries({ page: 0, size: 100 })
      const found = data.content.find((d) => d.entryDate === dateStr)
      if (found) {
        setEntry({
          id: found.id,
          userId: found.userId,
          entryDate: found.entryDate,
          content: found.content,
          emotionTag: found.emotionTag,
          streakCount: found.streakCount,
          createdAt: found.createdAt,
          updatedAt: found.createdAt,
        })
        setViewState("view")
      } else {
        // 활성 세션이 있는지 확인
        try {
          const activeSession = await getActiveSession(dateStr)
          setSession(activeSession)
          setChatHistory(activeSession.conversationHistory || [])

          if (activeSession.status === "GENERATED") {
            // 이미 생성됨 → 캐시에서 가져오기 시도
            const cached = localStorage.getItem(`diary_generated_${dateStr}`)
            if (cached) {
              const { content, emotion } = JSON.parse(cached)
              setCompiledContent(content)
              setSuggestedEmotion(emotion)
            } else {
              // 캐시 없으면 대화 내역 답변으로 대체
              const answers = (activeSession.conversationHistory || []).map(h => h.answer).filter(Boolean)
              setCompiledContent(answers.join("\n\n") || "일기가 생성되었습니다.")
              setSuggestedEmotion("CALM")
            }
            setViewState("compiled")
          } else if (activeSession.status === "READY_TO_GENERATE") {
            // 대화 완료, 생성 대기 → 생성 시작
            setViewState("chat")
            setCurrentQuestion("")
            setGenerating(true)
            try {
              const preview = await generateDiary(activeSession.sessionId)
              setCompiledContent(preview.generatedContent)
              setSuggestedEmotion(preview.suggestedEmotion)
              setViewState("compiled")
            } catch {
              // 생성 실패 시 대화 내역으로 대체
              const answers = (activeSession.conversationHistory || []).map(h => h.answer).filter(Boolean)
              setCompiledContent(answers.join("\n\n"))
              setViewState("compiled")
            } finally {
              setGenerating(false)
            }
          } else if (activeSession.status === "COMPLETED") {
            // 이미 확정됨 → 일기 목록 다시 로드
            setViewState("chat")
            setSession(null)
          } else {
            // IN_PROGRESS → 대화 계속
            setViewState("chat")
            setCurrentQuestion(activeSession.currentQuestion || "")
          }
        } catch {
          // 세션 없으면 새로 시작 가능
          setViewState("chat")
        }
      }
    } catch (err) {
      console.error("일기 조회 실패:", err)
      setViewState("chat")
    }
  }

  const handleStartAiSession = async () => {
    try {
      setAiLoading(true)
      const newSession = await startDiarySession(dateStr)
      setSession(newSession)
      setCurrentQuestion(newSession.currentQuestion || "오늘 하루는 어땠나요?")
    } catch (err) {
      console.error("AI 세션 시작 실패:", err)
    } finally {
      setAiLoading(false)
    }
  }

  const handleAnswerQuestion = async () => {
    if (!answerInput.trim() || !session) return
    try {
      setAiLoading(true)
      const answer = answerInput.trim()
      setChatHistory((prev) => [...prev, { question: currentQuestion, answer }])
      setAnswerInput("")
      setCurrentQuestion("") // 질문 비우기 → "..." 표시

      const result = await answerDiaryQuestion(session.sessionId, answer)

      if (result.isCompleted) {
        // 대화 완료 → 일기 생성
        setCurrentQuestion("")
        setGenerating(true)
        try {
          const preview = await generateDiary(session.sessionId)
          setCompiledContent(preview.generatedContent)
          setSuggestedEmotion(preview.suggestedEmotion)
          // 생성 결과를 로컬에 캐시 (재진입 시 사용)
          localStorage.setItem(`diary_generated_${dateStr}`, JSON.stringify({
            content: preview.generatedContent,
            emotion: preview.suggestedEmotion,
          }))
          setViewState("compiled")
        } finally {
          setGenerating(false)
        }
      } else {
        setCurrentQuestion(result.nextQuestion || "")
      }
    } catch (err) {
      console.error("답변 제출 실패:", err)
    } finally {
      setAiLoading(false)
    }
  }

  const handleConfirmDiary = async (content: string, emotion: EmotionTag) => {
    try {
      if (session) {
        await confirmDiary(session.sessionId, content, emotion)
      } else {
        await createDiary({ content, emotionTag: emotion, date: dateStr, source: "MANUAL" })
      }
      localStorage.removeItem(`diary_generated_${dateStr}`)
      router.push("/diary")
    } catch (err) {
      console.error("일기 저장 실패:", err)
    }
  }

  const handleManualSave = async () => {
    if (!manualContent.trim()) return
    try {
      await createDiary({ content: manualContent, emotionTag: manualEmotion, date: dateStr, source: "MANUAL" })
      router.push("/diary")
    } catch (err) {
      console.error("일기 저장 실패:", err)
    }
  }

  const handleBack = () => router.push("/diary")

  // Future
  if (viewState === "future") {
    return (
      <AppShell title="일기">
        <div className="p-4 flex flex-col items-center justify-center min-h-[60vh]">
          <div className="text-6xl mb-4">🔮</div>
          <h2 className="text-lg font-semibold text-foreground mb-2">아직 오지 않은 날이에요</h2>
          <p className="text-sm text-muted-foreground text-center mb-6">미래의 일기는 그 날이 되면 작성할 수 있어요</p>
          <Button onClick={handleBack}>돌아가기</Button>
        </div>
      </AppShell>
    )
  }

  // Loading
  if (viewState === "loading") {
    return (
      <AppShell title="일기">
        <div className="flex items-center justify-center h-40">
          <p className="text-sm text-muted-foreground">로딩 중...</p>
        </div>
      </AppShell>
    )
  }

  // View existing entry
  if (viewState === "view" && entry) {
    const emotionMeta = EMOTION_META[entry.emotionTag]
    return (
      <AppShell title="일기">
        <div className="p-4 space-y-4">
          <button onClick={handleBack} className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors">
            <ArrowLeft className="w-4 h-4" />
            <span className="text-sm">캘린더로 돌아가기</span>
          </button>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-2xl">{emotionMeta.weatherIcon}</span>
              <div>
                <h2 className="text-lg font-semibold text-foreground">{formatDisplayDate(entry.entryDate)}</h2>
                <p className="text-sm text-muted-foreground">{emotionMeta.label}</p>
              </div>
            </div>
          </div>

          <Card className={cn("border-0 shadow-md overflow-hidden", emotionMeta.bgColor)}>
            <CardContent className="p-0">
              <div className="p-6 min-h-[200px]" style={{ backgroundImage: "repeating-linear-gradient(transparent, transparent 27px, rgba(0,0,0,0.05) 28px)", backgroundSize: "100% 28px" }}>
                <p className="text-foreground/90 leading-7 whitespace-pre-wrap">{entry.content}</p>
              </div>
            </CardContent>
          </Card>

          {entry.streakCount > 0 && (
            <div className="flex items-center justify-center gap-2 py-3">
              <Sparkles className="w-4 h-4 text-amber-500" />
              <span className="text-sm text-muted-foreground">
                <span className="font-semibold text-amber-600">{entry.streakCount}일</span> 연속 작성
              </span>
            </div>
          )}
        </div>
      </AppShell>
    )
  }

  // Compiled view
  if (viewState === "compiled") {
    return (
      <AppShell title="일기 완성">
        <div className="p-4 space-y-4">
          <button onClick={async () => {
            // 기존 세션 취소 후 새로 시작
            if (session) {
              try { await cancelDiarySession(session.sessionId) } catch {}
            }
            setSession(null)
            setChatHistory([])
            setCurrentQuestion("")
            setCompiledContent("")
            localStorage.removeItem(`diary_generated_${dateStr}`)
            setViewState("chat")
          }} className="flex items-center gap-2 text-muted-foreground hover:text-foreground">
            <ArrowLeft className="w-4 h-4" />
            <span className="text-sm">다시 대화하기</span>
          </button>

          <h2 className="text-base font-semibold">AI가 작성한 일기</h2>

          <textarea
            value={compiledContent}
            onChange={(e) => setCompiledContent(e.target.value)}
            className="w-full min-h-[200px] p-4 rounded-xl border border-border/50 bg-background text-sm resize-none outline-none focus:ring-1 focus:ring-primary/30"
          />

          <div>
            <p className="text-xs text-muted-foreground mb-2">감정 태그</p>
            <div className="flex flex-wrap gap-2">
              {(["HAPPY", "SAD", "ANGRY", "ANXIOUS", "CALM", "EXCITED", "TIRED"] as EmotionTag[]).map((tag) => (
                <button
                  key={tag}
                  onClick={() => setSuggestedEmotion(tag)}
                  className={cn(
                    "px-3 py-1.5 rounded-full text-xs border transition-colors",
                    suggestedEmotion === tag
                      ? "border-primary bg-primary/10 text-primary"
                      : "border-border text-muted-foreground hover:bg-muted"
                  )}
                >
                  {EMOTION_META[tag].weatherIcon} {EMOTION_META[tag].label}
                </button>
              ))}
            </div>
          </div>

          <Button onClick={() => handleConfirmDiary(compiledContent, suggestedEmotion)} className="w-full h-11">
            일기 저장
          </Button>
        </div>
      </AppShell>
    )
  }

  // Chat view (AI or manual)
  return (
    <AppShell title="일기 쓰기">
      <div className="p-4 space-y-4 h-full flex flex-col">
        <button onClick={handleBack} className="flex items-center gap-2 text-muted-foreground hover:text-foreground shrink-0">
          <ArrowLeft className="w-4 h-4" />
          <span className="text-sm">캘린더</span>
        </button>

        <p className="text-base font-semibold shrink-0">{formatDisplayDate(dateStr)}</p>

        {/* Mode toggle */}
        <div className="flex gap-2 shrink-0">
          <Button
            size="sm"
            variant={session ? "default" : "outline"}
            onClick={handleStartAiSession}
            disabled={aiLoading || !!session}
            className="text-xs h-8 rounded-full"
          >
            ✨ AI와 대화로 쓰기
          </Button>
          <Button
            size="sm"
            variant={!session ? "default" : "outline"}
            onClick={() => setSession(null)}
            className="text-xs h-8 rounded-full"
          >
            ✏️ 직접 쓰기
          </Button>
        </div>

        {/* AI Chat */}
        {session ? (
          <div className="flex-1 flex flex-col min-h-0">
            {/* Chat history */}
            <div className="flex-1 overflow-y-auto space-y-3 mb-3">
              {chatHistory.map((item, i) => (
                <div key={i} className="space-y-2">
                  <div className="bg-secondary/50 rounded-xl p-3">
                    <p className="text-xs text-muted-foreground">Q.</p>
                    <p className="text-sm">{item.question}</p>
                  </div>
                  <div className="bg-primary/5 rounded-xl p-3 ml-4">
                    <p className="text-xs text-muted-foreground">A.</p>
                    <p className="text-sm">{item.answer}</p>
                  </div>
                </div>
              ))}
              {currentQuestion && currentQuestion.trim() !== "" && (
                <div className="bg-secondary/50 rounded-xl p-3">
                  <p className="text-xs text-muted-foreground">Q.</p>
                  <p className="text-sm">{currentQuestion}</p>
                </div>
              )}
              {!currentQuestion && aiLoading && !generating && (
                <div className="bg-secondary/50 rounded-xl p-3">
                  <p className="text-xs text-muted-foreground">Q.</p>
                  <div className="flex items-center gap-1 pt-1">
                    <span className="w-2 h-2 bg-muted-foreground/40 rounded-full animate-bounce [animation-delay:0ms]" />
                    <span className="w-2 h-2 bg-muted-foreground/40 rounded-full animate-bounce [animation-delay:150ms]" />
                    <span className="w-2 h-2 bg-muted-foreground/40 rounded-full animate-bounce [animation-delay:300ms]" />
                  </div>
                </div>
              )}
              {generating && (
                <div className="flex items-center justify-center py-6">
                  <div className="flex flex-col items-center gap-2">
                    <div className="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
                    <p className="text-sm text-muted-foreground">AI가 일기 생성중입니다...</p>
                  </div>
                </div>
              )}
              <div ref={chatEndRef} />
            </div>

            {/* Input */}
            <div className="shrink-0 flex gap-2">
              <input
                ref={inputRef}
                value={answerInput}
                onChange={(e) => setAnswerInput(e.target.value)}
                onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); handleAnswerQuestion() } }}
                placeholder="답변을 입력하세요..."
                className="flex-1 px-3 py-2 rounded-xl border border-border/50 text-sm outline-none focus:border-primary/50"
                disabled={aiLoading || generating}
              />
              <Button size="sm" onClick={handleAnswerQuestion} disabled={!answerInput.trim() || aiLoading || generating} className="h-9 px-4 rounded-xl">
                {aiLoading ? "..." : "전송"}
              </Button>
            </div>
          </div>
        ) : (
          /* Manual write */
          <div className="flex-1 flex flex-col min-h-0">
            <textarea
              value={manualContent}
              onChange={(e) => setManualContent(e.target.value)}
              placeholder="오늘 하루를 자유롭게 적어보세요..."
              className="flex-1 w-full p-4 rounded-xl border border-border/50 bg-background text-sm resize-none outline-none focus:ring-1 focus:ring-primary/30"
            />
            <div className="mt-3 shrink-0">
              <p className="text-xs text-muted-foreground mb-2">감정</p>
              <div className="flex flex-wrap gap-1.5 mb-3">
                {(["HAPPY", "SAD", "ANGRY", "ANXIOUS", "CALM", "EXCITED", "TIRED"] as EmotionTag[]).map((tag) => (
                  <button
                    key={tag}
                    onClick={() => setManualEmotion(tag)}
                    className={cn(
                      "px-2.5 py-1 rounded-full text-[11px] border transition-colors",
                      manualEmotion === tag
                        ? "border-primary bg-primary/10 text-primary"
                        : "border-border text-muted-foreground"
                    )}
                  >
                    {EMOTION_META[tag].weatherIcon} {EMOTION_META[tag].label}
                  </button>
                ))}
              </div>
              <Button onClick={handleManualSave} disabled={!manualContent.trim()} className="w-full h-10">
                저장
              </Button>
            </div>
          </div>
        )}
      </div>
    </AppShell>
  )
}
