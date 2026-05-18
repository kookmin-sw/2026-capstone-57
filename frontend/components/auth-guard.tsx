"use client"

import { useEffect, useState } from "react"
import { usePathname, useRouter } from "next/navigation"
import { onAuthFailure } from "@/lib/auth-event"

const PUBLIC_ROUTES = ["/login", "/signup", "/onboarding"]

export function AuthGuard({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const router = useRouter()
  const [status, setStatus] = useState<"loading" | "onboarding" | "authenticated">("loading")

  const isPublicRoute = PUBLIC_ROUTES.some(
    (route) => pathname === route || pathname.startsWith(route + "/")
  )

  useEffect(() => {
    if (isPublicRoute) {
      setStatus("authenticated")
      return
    }

    const token = localStorage.getItem("token")
    if (token) {
      setStatus("authenticated")
    } else {
      setStatus("onboarding")
    }
  }, [pathname, isPublicRoute])

  // API 레벨 401/403 감지 → 온보딩으로 이동
  useEffect(() => {
    const unsubscribe = onAuthFailure(() => {
      // 이미 public route에 있으면 무시 (무한 리다이렉트 방지)
      if (isPublicRoute) return
      router.replace("/onboarding")
    })
    return unsubscribe
  }, [router, isPublicRoute])

  useEffect(() => {
    if (status === "onboarding") {
      const timer = setTimeout(() => {
        router.replace("/login")
      }, 2500)
      return () => clearTimeout(timer)
    }
  }, [status, router])

  if (status === "loading") {
    return null
  }

  if (status === "onboarding") {
    return <OnboardingSplash />
  }

  return <>{children}</>
}

function OnboardingSplash() {
  return (
    <div className="h-screen bg-muted flex justify-center overflow-hidden">
      <div className="w-full max-w-[430px] h-full bg-background flex flex-col items-center justify-center relative shadow-xl">
        <div className="flex flex-col items-center gap-6 px-8 text-center animate-fade-in">
          {/* Logo */}
          <div className="text-6xl animate-bounce-in">🌤️</div>

          {/* Title */}
          <h1 className="text-2xl font-bold text-foreground animate-fade-in-delay-1">
            일기예보
          </h1>

          {/* Subtitle */}
          <p className="text-sm text-muted-foreground leading-relaxed animate-fade-in-delay-2">
            일기로 예견하는 보석같은 만남
            <br />
            캠퍼스에서 우연히 스친 인연을 발견하세요
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
