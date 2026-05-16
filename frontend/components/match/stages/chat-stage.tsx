"use client"

import { useState, useRef, useEffect } from "react"
import { Send, Lightbulb, Clock } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import type { ChatMessage } from "@/types/match"

interface ChatStageProps {
  messages: ChatMessage[]
  remainingTime: string
  onSendMessage: (content: string) => void
}

const icebreakers = [
  "요즘 가장 재밌게 본 영화나 드라마가 있어요?",
  "캠퍼스에서 좋아하는 장소가 어디예요?",
  "이번 주말에 뭐 할 계획이에요?",
]

export function ChatStage({ messages, remainingTime, onSendMessage }: ChatStageProps) {
  const [input, setInput] = useState("")
  const [currentIcebreaker, setCurrentIcebreaker] = useState(0)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages])

  const handleSend = () => {
    if (!input.trim()) return
    onSendMessage(input.trim())
    setInput("")
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const rotateIcebreaker = () => {
    setCurrentIcebreaker((prev) => (prev + 1) % icebreakers.length)
  }

  return (
    <div className="h-full bg-card rounded-2xl shadow-sm border border-border/30 flex flex-col overflow-hidden">
      {/* Header */}
      <div className="flex justify-between items-center px-3 py-2 border-b border-border/30 shrink-0">
        <h3 className="text-sm font-semibold text-foreground">2단계 · 채팅</h3>
        <div className="flex items-center gap-1 text-[11px] text-muted-foreground">
          <Clock className="w-3 h-3" />
          <span>{remainingTime}</span>
        </div>
      </div>

      {/* Icebreaker */}
      <div
        onClick={rotateIcebreaker}
        className="mx-3 mt-2 px-3 py-2 rounded-xl bg-secondary/40 cursor-pointer hover:bg-secondary/50 transition-colors shrink-0"
      >
        <div className="flex items-center gap-2">
          <Lightbulb className="w-3.5 h-3.5 text-accent shrink-0" />
          <p className="text-[11px] text-foreground/80 truncate">{icebreakers[currentIcebreaker]}</p>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-3 py-2 space-y-2 scrollbar-hide min-h-0">
        {messages.map((msg) => (
          <div
            key={msg.id}
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

      {/* Input */}
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
            disabled={!input.trim()}
            className="w-7 h-7 rounded-lg gradient-gem text-white border-0 shrink-0"
          >
            <Send className="w-3.5 h-3.5" />
          </Button>
        </div>
      </div>
    </div>
  )
}
