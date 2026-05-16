"use client"

import { useState, useRef, useEffect } from "react"
import { Send, Lightbulb, MessageSquare } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import type { ChatMessage } from "@/types/match"

interface ChatStageProps {
  messages: ChatMessage[]
  tokenLimit: number
  usedTokens: number
  icebreakerQuestion?: string
  isEnded?: boolean
  isConnected?: boolean
  onSendMessage: (content: string) => void
}

export function ChatStage({
  messages,
  tokenLimit,
  usedTokens,
  icebreakerQuestion,
  isEnded = false,
  isConnected = true,
  onSendMessage,
}: ChatStageProps) {
  const [input, setInput] = useState("")
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const remainingTokens = Math.max(0, tokenLimit - usedTokens)
  const usagePercent = tokenLimit > 0 ? Math.min(100, (usedTokens / tokenLimit) * 100) : 0

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages])

  const handleSend = () => {
    if (!input.trim() || isEnded || remainingTokens <= 0 || !isConnected) return
    onSendMessage(input.trim())
    setInput("")
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div className="h-full bg-card rounded-2xl shadow-sm border border-border/30 flex flex-col overflow-hidden">
      {/* Header */}
      <div className="flex justify-between items-center px-3 py-2 border-b border-border/30 shrink-0">
        <h3 className="text-sm font-semibold text-foreground">2단계 · 채팅</h3>
        <div className="flex items-center gap-1.5 text-[11px] text-muted-foreground">
          <MessageSquare className="w-3 h-3" />
          <span>{remainingTokens.toLocaleString()}자 남음</span>
        </div>
      </div>

      {/* Token usage bar */}
      <div className="px-3 pt-2 shrink-0">
        <div className="w-full h-1.5 rounded-full bg-secondary/50 overflow-hidden">
          <div
            className={cn(
              "h-full rounded-full transition-all duration-300",
              usagePercent >= 90 ? "bg-destructive" : usagePercent >= 70 ? "bg-amber-400" : "bg-primary"
            )}
            style={{ width: `${usagePercent}%` }}
          />
        </div>
        <p className="text-[10px] text-muted-foreground mt-0.5 text-right">
          {usedTokens.toLocaleString()} / {tokenLimit.toLocaleString()}자 사용
        </p>
      </div>

      {/* Icebreaker */}
      {icebreakerQuestion && messages.length === 0 && (
        <div className="mx-3 mt-2 px-3 py-2 rounded-xl bg-secondary/40 shrink-0">
          <div className="flex items-center gap-2">
            <Lightbulb className="w-3.5 h-3.5 text-accent shrink-0" />
            <p className="text-[11px] text-foreground/80">{icebreakerQuestion}</p>
          </div>
        </div>
      )}

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-3 py-2 space-y-2 scrollbar-hide min-h-0">
        {messages.map((msg, index) => (
          <div
            key={msg.id || `msg-${index}`}
            className={cn("flex", msg.isMe ? "justify-end" : "justify-start")}
          >
            <div className={cn(
              "max-w-[75%] px-3 py-2 rounded-2xl",
              msg.isMe
                ? "gradient-gem text-white"
                : "bg-secondary/70 text-foreground"
            )}>
              <p className="text-xs">{msg.content}</p>
              <p className={cn(
                "text-[9px] mt-0.5",
                msg.isMe ? "text-white/60" : "text-muted-foreground/60"
              )}>
                {msg.timestamp}
              </p>
            </div>
          </div>
        ))}
        <div ref={messagesEndRef} />
      </div>

      {/* Ended state */}
      {(isEnded || remainingTokens <= 0) && (
        <div className="px-3 py-2 border-t border-border/30 shrink-0">
          <p className="text-xs text-center text-muted-foreground">
            채팅이 종료되었습니다
          </p>
        </div>
      )}

      {/* Input */}
      {!isEnded && remainingTokens > 0 && (
        <div className="px-3 py-2 border-t border-border/30 shrink-0">
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl border border-border/50 bg-background">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="메시지를 입력하세요"
              className="flex-1 bg-transparent text-xs outline-none placeholder:text-muted-foreground"
            />
            <Button
              size="icon"
              onClick={handleSend}
              disabled={!input.trim() || !isConnected}
              className="w-7 h-7 rounded-lg gradient-gem text-white border-0 shrink-0"
            >
              <Send className="w-3.5 h-3.5" />
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
