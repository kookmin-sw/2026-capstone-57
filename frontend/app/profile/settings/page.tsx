"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { ArrowLeft, Moon, Sun, Smartphone, Database, Info } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"
import { Switch } from "@/components/ui/switch"
import { cn } from "@/lib/utils"

export default function SettingsPage() {
  const router = useRouter()

  const [darkMode, setDarkMode] = useState(false)
  const [autoLogin, setAutoLogin] = useState(true)
  const [dataUsage, setDataUsage] = useState(true)

  const handleClearCache = () => {
    if (confirm("캐시를 삭제하시겠습니까?")) {
      // 토큰과 userId는 유지
      const token = localStorage.getItem("token")
      const userId = localStorage.getItem("userId")
      const refreshToken = localStorage.getItem("refreshToken")

      // diary 캐시 등 삭제
      const keysToRemove: string[] = []
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i)
        if (key && key.startsWith("diary_generated_")) {
          keysToRemove.push(key)
        }
      }
      keysToRemove.forEach((key) => localStorage.removeItem(key))

      alert("캐시가 삭제되었습니다")
    }
  }

  return (
    <div className="h-screen bg-muted flex justify-center overflow-hidden">
      <div className="w-full max-w-[430px] h-full bg-background flex flex-col relative shadow-xl">
        {/* Header */}
        <div className="px-4 py-3 flex items-center gap-2 shrink-0 border-b border-border/50">
          <button onClick={() => router.push("/profile")} className="p-1 text-muted-foreground hover:text-foreground">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <h1 className="text-base font-semibold text-foreground">앱 설정</h1>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto px-5 py-5 space-y-5">
          {/* Display */}
          <Card className="border-0 shadow-sm">
            <CardContent className="p-4 space-y-4">
              <h3 className="text-sm font-medium text-foreground">화면</h3>

              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-muted flex items-center justify-center">
                    {darkMode ? <Moon className="w-4 h-4 text-foreground" /> : <Sun className="w-4 h-4 text-foreground" />}
                  </div>
                  <div>
                    <p className="text-sm text-foreground">다크 모드</p>
                    <p className="text-[11px] text-muted-foreground">어두운 테마 사용</p>
                  </div>
                </div>
                <Switch checked={darkMode} onCheckedChange={setDarkMode} />
              </div>
            </CardContent>
          </Card>

          {/* Account */}
          <Card className="border-0 shadow-sm">
            <CardContent className="p-4 space-y-4">
              <h3 className="text-sm font-medium text-foreground">계정</h3>

              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-muted flex items-center justify-center">
                    <Smartphone className="w-4 h-4 text-foreground" />
                  </div>
                  <div>
                    <p className="text-sm text-foreground">자동 로그인</p>
                    <p className="text-[11px] text-muted-foreground">앱 실행 시 자동 로그인</p>
                  </div>
                </div>
                <Switch checked={autoLogin} onCheckedChange={setAutoLogin} />
              </div>

              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-muted flex items-center justify-center">
                    <Database className="w-4 h-4 text-foreground" />
                  </div>
                  <div>
                    <p className="text-sm text-foreground">데이터 절약</p>
                    <p className="text-[11px] text-muted-foreground">이미지 품질 낮춤</p>
                  </div>
                </div>
                <Switch checked={dataUsage} onCheckedChange={setDataUsage} />
              </div>
            </CardContent>
          </Card>

          {/* Storage */}
          <Card className="border-0 shadow-sm">
            <CardContent className="p-4 space-y-4">
              <h3 className="text-sm font-medium text-foreground">저장소</h3>

              <button
                onClick={handleClearCache}
                className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-muted/50 transition-colors"
              >
                <div className="w-8 h-8 rounded-lg bg-muted flex items-center justify-center">
                  <Database className="w-4 h-4 text-foreground" />
                </div>
                <div className="text-left">
                  <p className="text-sm text-foreground">캐시 삭제</p>
                  <p className="text-[11px] text-muted-foreground">임시 데이터 삭제</p>
                </div>
              </button>
            </CardContent>
          </Card>

          {/* App Info */}
          <Card className="border-0 shadow-sm">
            <CardContent className="p-4 space-y-3">
              <h3 className="text-sm font-medium text-foreground">앱 정보</h3>

              <div className="flex items-center justify-between py-1">
                <span className="text-sm text-muted-foreground">버전</span>
                <span className="text-sm text-foreground">1.0.0</span>
              </div>
              <div className="flex items-center justify-between py-1">
                <span className="text-sm text-muted-foreground">빌드</span>
                <span className="text-sm text-foreground">2026.05.15</span>
              </div>
              <div className="flex items-center justify-between py-1">
                <span className="text-sm text-muted-foreground">개발</span>
                <span className="text-sm text-foreground">캡스톤 57조</span>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
