"use client"

import { useState, useEffect, useCallback } from "react"
import { MessageCircle, X, Send } from "lucide-react"
import { Button } from "@/components/ui/button"
import { getHints, answerHint, type HintQuestionDto } from "@/lib/api/interaction"

interface HintNotificationProps {
  matchId: string
  pollInterval?: number
}

export function HintNotification({ matchId, pollInterval = 15000 }: HintNotificationProps) {
  const [pendingHint, setPendingHint] = useState<HintQuestionDto | null>(null)
  const [showBanner, setShowBanner] = useState(false)
  const [showModal, setShowModal] = useState(false)
  const [answer, setAnswer] = useState("")
  const [submitting, setSubmitting] = useState(false)
  const [submitted, setSubmitted] = useState(false)

  const checkHints = useCallback(async () => {
    if (typeof window === "undefined") return
    const userId = localStorage.getItem("userId")
    if (!userId) return

    try {
      const hints = await getHints(matchId)
      const mine = hints.find(
        (h) => h.responderId === userId && h.status === "PENDING"
      )
      if (mine && mine.id !== pendingHint?.id) {
        setPendingHint(mine)
        setShowBanner(true)
        setSubmitted(false)
      }
    } catch (err) {
      console.error("힌트 조회 실패:", err)
    }
  }, [matchId, pendingHint?.id])

  useEffect(() => {
    checkHints()
    const timer = setInterval(checkHints, pollInterval)
    return () => clearInterval(timer)
  }, [checkHints, pollInterval])

  const handleOpenModal = () => {
    setShowBanner(false)
    setShowModal(true)
  }

  const handleSubmit = async () => {
    if (!pendingHint || !answer.trim()) return
    setSubmitting(true)
    try {
      await answerHint(matchId, pendingHint.id, answer.trim())
      setSubmitted(true)
      setAnswer("")
      setTimeout(() => {
        setShowModal(false)
        setPendingHint(null)
      }, 1200)
    } catch (err) {
      console.error("힌트 답변 실패:", err)
    } finally {
      setSubmitting(false)
    }
  }

  const handleCloseModal = () => {
    setShowModal(false)
    if (pendingHint && !submitted) {
      setShowBanner(true)
    }
  }

  return (
    <>
      {/* 인라인 배너 — 원하는 위치에 렌더링됨 */}
      {showBanner && pendingHint && (
        <div className="flex items-center gap-3 bg-card border border-border/40 rounded-2xl px-4 py-3">
          <div className="flex items-center justify-center w-8 h-8 rounded-full bg-primary/10 shrink-0">
            <MessageCircle className="w-4 h-4 text-primary" />
          </div>

          <button
            type="button"
            className="flex-1 text-left"
            onClick={handleOpenModal}
          >
            <p className="text-xs font-semibold text-foreground">힌트 질문이 도착했어요!</p>
            <p className="text-[11px] text-muted-foreground mt-0.5 line-clamp-1">
              {pendingHint.question}
            </p>
          </button>

          <button
            type="button"
            onClick={() => setShowBanner(false)}
            className="shrink-0 p-1 rounded-full hover:bg-secondary/60"
            aria-label="닫기"
          >
            <X className="w-3.5 h-3.5 text-muted-foreground" />
          </button>
        </div>
      )}

      {/* 답변 모달 — fixed는 모달만 유지 */}
      {showModal && pendingHint && (
        <div
          className="fixed inset-0 z-50 flex items-end justify-center bg-black/30 backdrop-blur-[2px]"
          onClick={(e) => { if (e.target === e.currentTarget) handleCloseModal() }}
        >
          <div className="w-full max-w-md bg-card rounded-t-3xl px-5 pb-24 pt-5 shadow-xl">
            <div className="w-10 h-1 rounded-full bg-border mx-auto mb-5" />

            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-semibold text-foreground">힌트 질문 답변하기</h2>
              <button
                type="button"
                onClick={handleCloseModal}
                className="p-1.5 rounded-full hover:bg-secondary/60"
                aria-label="닫기"
              >
                <X className="w-4 h-4 text-muted-foreground" />
              </button>
            </div>

            <div className="bg-secondary/40 rounded-2xl px-4 py-3 mb-4">
              <p className="text-[11px] text-muted-foreground mb-1">상대방의 질문</p>
              <p className="text-sm text-foreground font-medium">{pendingHint.question}</p>
            </div>

            {submitted ? (
              <div className="flex flex-col items-center justify-center py-6 gap-2">
                <div className="w-10 h-10 rounded-full bg-green-50 flex items-center justify-center">
                  <Send className="w-5 h-5 text-green-500" />
                </div>
                <p className="text-sm text-green-600 font-medium">답변을 전송했어요!</p>
              </div>
            ) : (
              <>
                <textarea
                  value={answer}
                  onChange={(e) => setAnswer(e.target.value)}
                  placeholder="편하게 답변해 보아요~"
                  className="w-full bg-muted/50 text-sm resize-none outline-none placeholder:text-muted-foreground/50 rounded-xl p-3 min-h-[100px] border border-border/30 focus:border-primary/50 transition-colors"
                  rows={4}
                  autoFocus
                />
                <Button
                  type="button"
                  onClick={handleSubmit}
                  disabled={!answer.trim() || submitting}
                  className="w-full h-11 mt-3 rounded-2xl text-sm font-medium"
                >
                  <Send className="w-4 h-4 mr-2" />
                  {submitting ? "전송 중..." : "답변 보내기"}
                </Button>
              </>
            )}
          </div>
        </div>
      )}
    </>
  )
}