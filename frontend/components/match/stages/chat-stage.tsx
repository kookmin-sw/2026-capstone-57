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
    <div className="bg-card rounded-3xl shadow-sm border border-border/30 flex flex-col h-[400px]">
      {/* Header */}
      <div className="flex justify-between items-center p-4 border-b border-border/30">
        <h3 className="text-base font-semibold text-foreground">2단계 · 채팅</h3>
        <div className="flex items-center gap-1 text-xs text-muted-foreground">
          <Clock className="w-3.5 h-3.5" />
          <span>남은 시간 {remainingTime}</span>
        </div>
      </div>

      {/* Icebreaker */}
      <div 
        onClick={rotateIcebreaker}
        className="mx-4 mt-3 p-3 rounded-2xl bg-secondary/40 cursor-pointer hover:bg-secondary/50 transition-colors"
      >
        <div className="flex items-start gap-2">
          <Lightbulb className="w-4 h-4 text-accent shrink-0 mt-0.5" />
          <p className="text-xs text-foreground/80">{icebreakers[currentIcebreaker]}</p>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3 scrollbar-hide">
        {messages.map((msg) => (
          <div 
            key={msg.id}
            className={cn(
              "flex",
              msg.isMe ? "justify-end" : "justify-start"
            )}
          >
            <div className={cn(
              "max-w-[75%] px-3.5 py-2.5 rounded-2xl",
              msg.isMe 
                ? "gradient-gem text-white" 
                : "bg-secondary/70 text-foreground"
            )}>
              <p className="text-sm">{msg.content}</p>
              <p className={cn(
                "text-[10px] mt-1",
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
      <div className="p-3 border-t border-border/30">
        <div className="flex items-center gap-2 p-2 rounded-2xl border border-border/50 bg-background">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="메시지를 입력하세요"
            className="flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
          />
          <Button
            size="icon"
            onClick={handleSend}
            disabled={!input.trim()}
            className="w-9 h-9 rounded-xl gradient-gem text-white border-0 shadow-gem shrink-0"
          >
            <Send className="w-4 h-4" />
          </Button>
        </div>
      </div>
    </div>
  )
}
