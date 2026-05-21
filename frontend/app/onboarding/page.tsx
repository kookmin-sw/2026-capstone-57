"use client"

import Image from "next/image"
import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"

export default function OnboardingPage() {
  const router = useRouter()
  const subtitles = [
    "일기로 예견하는 보석같은 만남\n캠퍼스에서 우연히 스친 인연을 발견하세요",
    "같은 시간, 같은 장소를 지나쳤다면\n그 우연을 일기예보가 인연으로 이어줄게요",
    "오늘의 만남 예보\n캠퍼스에서 스친 인연을 발견하는 특별한 경험",
    "오늘 일기에 적힐 만남은 누구일까요?\n캠퍼스에서 우연히 스친 인연을 발견하세요",
    "일기로 찾는 오늘의 인연\n캠퍼스에서 스친 인연을 발견하는 특별한 경험"
  ]
  const [subtitle] = useState(() => subtitles[Math.floor(Math.random() * subtitles.length)])

  useEffect(() => {
    const timer = setTimeout(() => {
      router.replace("/login")
    }, 2500)
    return () => clearTimeout(timer)
  }, [router])

  return (
    <div className="h-screen bg-muted flex justify-center overflow-hidden">
      <div className="w-full max-w-[430px] h-full bg-background flex flex-col items-center justify-center relative shadow-xl">
        <div className="flex flex-col items-center gap-6 px-8 text-center animate-fade-in">
          {/* Logo */}
          <div className="animate-bounce-in">
            <Image
              src="/logo.png"
              alt="일기예보 로고"
              width={96}
              height={96}
              priority
              className="h-24 w-24 object-contain"
            />
          </div>

          {/* Title */}
          <h1 className="text-2xl font-bold text-foreground animate-fade-in-delay-1">
            일기예보
          </h1>

          {/* Subtitle */}
          <p className="text-sm text-muted-foreground leading-relaxed animate-fade-in-delay-2">
            {subtitle.split("\n").map((line, index) => (
              <span key={`${line}-${index}`}>
                {line}
                <br />
              </span>
            ))}
          </p>

          {/* Loading indicator */}
          <div className="mt-8 flex items-center gap-2 text-xs text-muted-foreground animate-fade-in-delay-3">
            <div className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse" />
            <span>로그인 화면으로 이동합니다...</span>
          </div>
        </div>
      </div>
    </div>
  )
}
