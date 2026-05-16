"use client"

import { useState, useRef, useEffect } from "react"
import { Send, Check, Sparkles } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import type { ChatMessage, SessionStatus } from "@/types/diary-chat"
import { AI_CHARACTER, SAMPLE_AI_QUESTIONS } from "@/types/diary-chat"

interface DiaryAIChatProps {
  date: Date
  onComplete: (messages: ChatMessage[]) => void
  className?: string
}

// AI 메시지 버블
function AIMessageBubble({ content }: { content: string }) {
  return (
    <div className="flex items-start gap-2 max-w-[85%]">
      <div className="size-8 rounded-full bg-secondary flex items-center justify-center shrink-0 text-base">
        {AI_CHARACTER.avatar}
      </div>
      <div className="flex flex-col gap-1">
        <span className="text-xs text-muted-foreground font-medium">{AI_CHARACTER.name}</span>
        <div className="bg-card border border-border/50 rounded-2xl rounded-tl-sm px-4 py-2.5 shadow-sm">
          <p className="text-sm text-foreground leading-relaxed">{content}</p>
        </div>
      </div>
    </div>
  )
}

// 사용자 메시지 버블
function UserMessageBubble({ content }: { content: string }) {
  return (
    <div className="flex justify-end">
      <div className="max-w-[85%] bg-primary text-primary-foreground rounded-2xl rounded-tr-sm px-4 py-2.5 shadow-sm">
        <p className="text-sm leading-relaxed">{content}</p>
      </div>
    </div>
  )
}

// 타이핑 인디케이터
function TypingIndicator() {
  return (
    <div className="flex items-start gap-2 max-w-[85%]">
      <div className="size-8 rounded-full bg-secondary flex items-center justify-center shrink-0 text-base">
        {AI_CHARACTER.avatar}
      </div>
      <div className="flex flex-col gap-1">
        <span className="text-xs text-muted-foreground font-medium">{AI_CHARACTER.name}</span>
        <div className="bg-card border border-border/50 rounded-2xl rounded-tl-sm px-4 py-3 shadow-sm">
          <div className="flex gap-1">
            <span className="size-2 bg-muted-foreground/50 rounded-full animate-bounce [animation-delay:0ms]" />
            <span className="size-2 bg-muted-foreground/50 rounded-full animate-bounce [animation-delay:150ms]" />
            <span className="size-2 bg-muted-foreground/50 rounded-full animate-bounce [animation-delay:300ms]" />
          </div>
        </div>
      </div>
    </div>
  )
}

// 진행 바
function ProgressBar({ current, max }: { current: number; max: number }) {
  const progress = (current / max) * 100
  return (
    <div className="flex items-center gap-3 px-1">
      <div className="flex-1 h-1.5 bg-muted rounded-full overflow-hidden">
        <div 
          className="h-full bg-primary rounded-full transition-all duration-500 ease-out"
          style={{ width: `${progress}%` }}
        />
      </div>
      <span className="text-xs text-muted-foreground font-medium shrink-0">
        {current}/{max}
      </span>
    </div>
  )
}

export function DiaryAIChat({ date, onComplete, className }: DiaryAIChatProps) {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState("")
  const [isTyping, setIsTyping] = useState(false)
  const [currentTurn, setCurrentTurn] = useState(1)
  const [status, setStatus] = useState<SessionStatus>("active")
  const maxTurns = 5
  
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLTextAreaElement>(null)

  // 스크롤을 맨 아래로
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages, isTyping])

  // 초기 AI 인사말
  useEffect(() => {
    const timer = setTimeout(() => {
      setIsTyping(true)
      setTimeout(() => {
        setIsTyping(false)
        setMessages([{
          id: "ai-0",
          role: "ai",
          content: AI_CHARACTER.greeting,
          timestamp: new Date(),
        }])
      }, 1000)
    }, 500)
    return () => clearTimeout(timer)
  }, [])

  // AI 응답 시뮬레이션
  const simulateAIResponse = (userMessage: string) => {
    setIsTyping(true)
    
    // 실제로는 API 호출
    setTimeout(() => {
      setIsTyping(false)
      const nextQuestion = SAMPLE_AI_QUESTIONS[currentTurn] || "또 다른 얘기 있어?"
      
      setMessages(prev => [...prev, {
        id: `ai-${currentTurn}`,
        role: "ai",
        content: nextQuestion,
        timestamp: new Date(),
      }])
      
      setCurrentTurn(prev => prev + 1)
    }, 1500)
  }

  // 메시지 전송
  const handleSend = () => {
    if (!input.trim() || isTyping || status !== "active") return

    const userMessage: ChatMessage = {
      id: `user-${Date.now()}`,
      role: "user",
      content: input.trim(),
      timestamp: new Date(),
    }

    setMessages(prev => [...prev, userMessage])
    setInput("")

    // 최대 턴에 도달하면 완료 처리
    if (currentTurn >= maxTurns) {
      handleComplete()
    } else {
      simulateAIResponse(input)
    }
  }

  // 완료 처리
  const handleComplete = () => {
    if (messages.length < 2) return // 최소 1번의 대화는 필요
    
    setStatus("completing")
    setIsTyping(true)
    
    // AI가 일기 컴파일 중인 것처럼 시뮬레이션
    setTimeout(() => {
      setIsTyping(false)
      setMessages(prev => [...prev, {
        id: "ai-complete",
        role: "ai",
        content: "좋아, 오늘 하루 얘기 잘 들었어! 일기를 정리해볼게 ✨",
        timestamp: new Date(),
      }])
      setStatus("completed")
      
      // 잠시 후 완료 콜백
      setTimeout(() => {
        onComplete(messages)
      }, 1500)
    }, 2000)
  }

  // 엔터 키 처리
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const canComplete = messages.filter(m => m.role === "user").length >= 1

  return (
    <div className={cn("flex flex-col h-full", className)}>
      {/* 진행 바 */}
      <div className="px-4 py-3 border-b border-border/30 bg-card/50">
        <ProgressBar current={currentTurn} max={maxTurns} />
      </div>

      {/* 메시지 영역 */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4 scrollbar-hide">
        {messages.map((message) => (
          message.role === "ai" 
            ? <AIMessageBubble key={message.id} content={message.content} />
            : <UserMessageBubble key={message.id} content={message.content} />
        ))}
        
        {isTyping && <TypingIndicator />}
        
        <div ref={messagesEndRef} />
      </div>

      {/* 입력 영역 */}
      {status === "active" && (
        <div className="p-4 border-t border-border/30 bg-card/80 backdrop-blur-sm">
          <div className="flex gap-2">
            <div className="flex-1 relative">
              <textarea
                ref={inputRef}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="오늘 있었던 일을 얘기해줘..."
                className={cn(
                  "w-full resize-none rounded-xl border border-border bg-background px-4 py-3 pr-12",
                  "text-sm placeholder:text-muted-foreground",
                  "focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary",
                  "min-h-[48px] max-h-[120px]"
                )}
                rows={1}
                disabled={isTyping}
              />
              <Button
                size="icon"
                className="absolute right-2 bottom-2 size-8 rounded-lg"
                onClick={handleSend}
                disabled={!input.trim() || isTyping}
              >
                <Send className="size-4" />
              </Button>
            </div>
          </div>
          
          {/* 완료 버튼 */}
          {canComplete && (
            <Button
              variant="ghost"
              size="sm"
              className="w-full mt-2 text-muted-foreground hover:text-foreground"
              onClick={handleComplete}
              disabled={isTyping}
            >
              <Check className="size-4 mr-1.5" />
              여기까지 작성하기
            </Button>
          )}
        </div>
      )}

      {/* 완료 중 상태 */}
      {status === "completing" && (
        <div className="p-4 border-t border-border/30 bg-card/80">
          <div className="flex items-center justify-center gap-2 text-muted-foreground py-4">
            <Sparkles className="size-4 animate-pulse" />
            <span className="text-sm">일기를 정리하고 있어요...</span>
          </div>
        </div>
      )}

      {/* 완료됨 상태 */}
      {status === "completed" && (
        <div className="p-4 border-t border-border/30 bg-card/80">
          <div className="flex items-center justify-center gap-2 text-primary py-4">
            <Sparkles className="size-4" />
            <span className="text-sm font-medium">일기 정리 완료!</span>
          </div>
        </div>
      )}
    </div>
  )
}
