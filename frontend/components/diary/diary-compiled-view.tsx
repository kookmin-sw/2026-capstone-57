"use client"

import { useState } from "react"
import { Edit3, Check, Sparkles } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import type { EmotionTag } from "@/types/diary"
import { EMOTION_META } from "@/types/diary"

interface DiaryCompiledViewProps {
  content: string
  suggestedEmotion: EmotionTag
  summary: string
  date: Date
  onSave: (content: string, emotion: EmotionTag) => void
  onEdit: () => void
  className?: string
}

// 감정 선택기
function EmotionPicker({ 
  selected, 
  onChange 
}: { 
  selected: EmotionTag
  onChange: (emotion: EmotionTag) => void 
}) {
  const emotions = Object.entries(EMOTION_META) as [EmotionTag, typeof EMOTION_META[EmotionTag]][]
  
  return (
    <div className="flex flex-wrap gap-2 justify-center">
      {emotions.map(([tag, meta]) => (
        <button
          key={tag}
          onClick={() => onChange(tag)}
          className={cn(
            "flex items-center gap-1.5 px-3 py-1.5 rounded-full text-sm transition-all",
            "border",
            selected === tag
              ? cn(meta.bgColor, "border-current", meta.color, "shadow-sm")
              : "border-border bg-card hover:bg-muted"
          )}
        >
          <span>{meta.weatherIcon}</span>
          <span className={selected === tag ? meta.color : "text-muted-foreground"}>
            {meta.label}
          </span>
        </button>
      ))}
    </div>
  )
}

export function DiaryCompiledView({
  content,
  suggestedEmotion,
  summary,
  date,
  onSave,
  onEdit,
  className,
}: DiaryCompiledViewProps) {
  const [selectedEmotion, setSelectedEmotion] = useState<EmotionTag>(suggestedEmotion)
  const [isEditing, setIsEditing] = useState(false)
  const [editedContent, setEditedContent] = useState(content)

  const emotionMeta = EMOTION_META[selectedEmotion]
  
  const formatDate = (d: Date) => {
    const year = d.getFullYear()
    const month = d.getMonth() + 1
    const day = d.getDate()
    const weekdays = ["일", "월", "화", "수", "목", "금", "토"]
    const weekday = weekdays[d.getDay()]
    return `${year}년 ${month}월 ${day}일 ${weekday}요일`
  }

  const handleSave = () => {
    onSave(editedContent, selectedEmotion)
  }

  return (
    <div className={cn("flex flex-col h-full p-4 space-y-4", className)}>
      {/* 헤더 */}
      <div className="text-center space-y-1">
        <div className="flex items-center justify-center gap-2">
          <Sparkles className="size-5 text-primary" />
          <h2 className="text-lg font-semibold text-foreground">일기가 완성됐어요!</h2>
        </div>
        <p className="text-sm text-muted-foreground">{formatDate(date)}</p>
      </div>

      {/* AI 요약 */}
      <div className="bg-secondary/30 rounded-xl px-4 py-3 text-center">
        <p className="text-sm text-foreground/80 italic">&ldquo;{summary}&rdquo;</p>
      </div>

      {/* 감정 선택 */}
      <div className="space-y-2">
        <p className="text-xs text-muted-foreground text-center">오늘의 감정</p>
        <EmotionPicker selected={selectedEmotion} onChange={setSelectedEmotion} />
      </div>

      {/* 일기 내용 카드 */}
      <Card className={cn(
        "flex-1 border-0 shadow-md overflow-hidden",
        emotionMeta.bgColor
      )}>
        <CardContent className="p-0 h-full">
          <div 
            className="p-5 h-full overflow-y-auto"
            style={{
              backgroundImage: "repeating-linear-gradient(transparent, transparent 27px, rgba(0,0,0,0.05) 28px)",
              backgroundSize: "100% 28px",
            }}
          >
            {isEditing ? (
              <textarea
                value={editedContent}
                onChange={(e) => setEditedContent(e.target.value)}
                className="w-full h-full bg-transparent text-foreground/90 leading-7 resize-none focus:outline-none"
                autoFocus
              />
            ) : (
              <p className="text-foreground/90 leading-7 whitespace-pre-wrap">
                {editedContent}
              </p>
            )}
          </div>
        </CardContent>
      </Card>

      {/* 액션 버튼 */}
      <div className="flex gap-3">
        <Button
          variant="outline"
          className="flex-1"
          onClick={() => setIsEditing(!isEditing)}
        >
          <Edit3 className="size-4 mr-2" />
          {isEditing ? "미리보기" : "수정하기"}
        </Button>
        <Button
          className="flex-1"
          onClick={handleSave}
        >
          <Check className="size-4 mr-2" />
          저장하기
        </Button>
      </div>
    </div>
  )
}
