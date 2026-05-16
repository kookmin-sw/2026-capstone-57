"use client"

import { useState } from "react"
import { X } from "lucide-react"
import { cn } from "@/lib/utils"

interface HintNotificationBannerProps {
  question?: string
  className?: string
}

export function HintNotificationBanner({
  question = "요즘 가장 자주 가는 장소는 어디인가요?",
  className,
}: HintNotificationBannerProps) {
  const [isModalOpen, setIsModalOpen] = useState(false)

  return (
    <>
      {/* Banner */}
      <button
        type="button"
        onClick={() => setIsModalOpen(true)}
        className={cn(
          "w-full flex items-center gap-2 px-4 py-2.5 rounded-xl",
          "bg-amber-50 border border-amber-200/60",
          "hover:bg-amber-100/80 transition-colors text-left",
          className
        )}
      >
        <span className="text-base">💌</span>
        <p className="text-xs text-amber-700 font-medium flex-1">
          힌트 질문이 도착했어요
        </p>
        <span className="text-xs text-amber-500">보기 →</span>
      </button>

      {/* Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="absolute inset-0 bg-black/40"
            onClick={() => setIsModalOpen(false)}
          />
          <div className="relative bg-white rounded-2xl p-5 w-full max-w-sm shadow-xl">
            <button
              type="button"
              onClick={() => setIsModalOpen(false)}
              className="absolute top-3 right-3 text-muted-foreground hover:text-foreground"
            >
              <X className="w-4 h-4" />
            </button>
            <div className="flex items-start gap-2 mb-4">
              <span className="text-xl">💌</span>
              <div>
                <p className="text-sm font-semibold text-foreground mb-1">
                  힌트 질문이 도착했어요
                </p>
                <p className="text-xs text-muted-foreground">
                  상대방이 궁금한 점을 물어봤어요
                </p>
              </div>
            </div>
            <div className="bg-amber-50/80 rounded-xl p-4 border border-amber-200/40">
              <p className="text-sm text-foreground leading-relaxed">
                &quot;{question}&quot;
              </p>
            </div>
            <button
              type="button"
              onClick={() => setIsModalOpen(false)}
              className="w-full mt-4 py-2.5 rounded-full bg-amber-500 hover:bg-amber-600 text-white text-sm font-medium transition-colors"
            >
              답변하러 가기
            </button>
          </div>
        </div>
      )}
    </>
  )
}
