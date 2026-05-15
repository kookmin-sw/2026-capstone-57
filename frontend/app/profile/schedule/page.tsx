"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { ArrowLeft, Clock, MapPin } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent } from "@/components/ui/card"
import { apiFetch } from "@/lib/api/client"
import { cn } from "@/lib/utils"

interface ScheduleItem {
  id: string
  name: string
  dayOfWeek: string
  startedAt: { hour: number; minute: number }
  endedAt: { hour: number; minute: number }
  place: string
}

interface ScheduleResponse {
  schedules: ScheduleItem[]
}

interface ScheduleUpsertResponse {
  schedules: ScheduleItem[]
  plannerResult: {
    createdCount: number
    skippedCount: number
  }
}

const DAY_LABELS: Record<string, string> = {
  MONDAY: "월",
  TUESDAY: "화",
  WEDNESDAY: "수",
  THURSDAY: "목",
  FRIDAY: "금",
  SATURDAY: "토",
  SUNDAY: "일",
}

function formatTime(t: any): string {
  if (!t) return ""
  if (typeof t === "string") return t.substring(0, 5)
  if (typeof t === "object" && t.hour !== undefined) {
    return `${t.hour.toString().padStart(2, "0")}:${t.minute.toString().padStart(2, "0")}`
  }
  return ""
}

export default function SchedulePage() {
  const router = useRouter()
  const [identifier, setIdentifier] = useState("")
  const [loading, setLoading] = useState(false)
  const [schedules, setSchedules] = useState<ScheduleItem[]>([])
  const [loadingSchedules, setLoadingSchedules] = useState(true)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")

  // 기존 시간표 조회
  useEffect(() => {
    async function load() {
      try {
        const data = await apiFetch<ScheduleResponse>("/api/users/me/schedules")
        setSchedules(data.schedules || [])
      } catch (err) {
        console.error("시간표 조회 실패:", err)
      } finally {
        setLoadingSchedules(false)
      }
    }
    load()
  }, [])

  // 에브리타임 시간표 등록
  const handleRegister = async () => {
    if (!identifier.trim()) {
      setError("에브리타임 시간표 URL을 입력해주세요")
      return
    }
    // URL 또는 식별자에서 @ 뒷부분 추출
    let parsed = identifier.trim()
    if (parsed.includes("@")) {
      parsed = parsed.split("@").pop() || ""
    }
    if (!parsed) {
      setError("올바른 URL 또는 식별자를 입력해주세요")
      return
    }
    try {
      setLoading(true)
      setError("")
      setSuccess("")
      const data = await apiFetch<ScheduleUpsertResponse>(
        `/api/users/me/schedules/${encodeURIComponent(parsed)}`,
        { method: "POST" }
      )
      setSchedules(data.schedules || [])
      setSuccess(`시간표 등록 완료! ${data.plannerResult.createdCount}개 일정이 플래너에 추가되었습니다.`)
      setIdentifier("")
    } catch (err: any) {
      console.error("시간표 등록 실패:", err)
      setError("시간표 등록에 실패했습니다. URL을 확인해주세요.")
    } finally {
      setLoading(false)
    }
  }

  // 요일 순서 정렬
  const dayOrder = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]
  const sortedSchedules = [...schedules].sort((a, b) => {
    const dayDiff = dayOrder.indexOf(a.dayOfWeek) - dayOrder.indexOf(b.dayOfWeek)
    if (dayDiff !== 0) return dayDiff
    const aStart = typeof a.startedAt === "string" ? a.startedAt : `${a.startedAt?.hour || 0}:${a.startedAt?.minute || 0}`
    const bStart = typeof b.startedAt === "string" ? b.startedAt : `${b.startedAt?.hour || 0}:${b.startedAt?.minute || 0}`
    return aStart.localeCompare(bStart)
  })

  return (
    <div className="h-screen bg-muted flex justify-center overflow-hidden">
      <div className="w-full max-w-[430px] h-full bg-background flex flex-col relative shadow-xl">
        {/* Header */}
        <div className="px-4 py-3 flex items-center gap-2 shrink-0 border-b border-border/50">
          <button onClick={() => router.push("/profile")} className="p-1 text-muted-foreground hover:text-foreground">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <h1 className="text-base font-semibold text-foreground">시간표 등록</h1>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto px-5 py-5 space-y-5">
          {/* Register */}
          <Card className="border-0 shadow-sm">
            <CardContent className="p-4 space-y-3">
              <div>
                <h3 className="text-sm font-semibold text-foreground">에브리타임 연동</h3>
                <p className="text-[11px] text-muted-foreground mt-0.5">
                  에브리타임 시간표 공유 URL의 식별자를 입력하세요
                </p>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">식별자</Label>
                <Input
                  placeholder="https://everytime.kr/@bvG43GaA..."
                  value={identifier}
                  onChange={(e) => setIdentifier(e.target.value)}
                  className="mt-1 h-11 text-base"
                />
                <p className="text-[10px] text-muted-foreground mt-1">
                  에브리타임 → 시간표 → 공유 → URL 복사 후 붙여넣기
                </p>
              </div>

              {error && <p className="text-xs text-destructive">{error}</p>}
              {success && <p className="text-xs text-green-600">{success}</p>}

              <Button
                onClick={handleRegister}
                disabled={loading}
                className="w-full h-10 text-sm"
              >
                {loading ? "등록 중..." : "시간표 가져오기"}
              </Button>
            </CardContent>
          </Card>

          {/* Current Schedule */}
          <div>
            <h3 className="text-sm font-semibold text-foreground mb-3">현재 등록된 시간표</h3>
            {loadingSchedules ? (
              <p className="text-xs text-muted-foreground text-center py-4">로딩 중...</p>
            ) : sortedSchedules.length === 0 ? (
              <p className="text-xs text-muted-foreground text-center py-4">등록된 시간표가 없습니다</p>
            ) : (
              <div className="space-y-2">
                {sortedSchedules.map((item) => (
                  <div
                    key={item.id}
                    className="flex items-center gap-3 px-3 py-2.5 rounded-xl bg-muted/50 border border-border/50"
                  >
                    <div className="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center shrink-0">
                      <span className="text-xs font-bold text-primary">
                        {DAY_LABELS[item.dayOfWeek] || item.dayOfWeek.charAt(0)}
                      </span>
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-medium text-foreground truncate">{item.name}</p>
                      <div className="flex items-center gap-2 mt-0.5 text-[11px] text-muted-foreground">
                        <span className="flex items-center gap-0.5">
                          <Clock className="w-2.5 h-2.5" />
                          {formatTime(item.startedAt)} - {formatTime(item.endedAt)}
                        </span>
                        {item.place && (
                          <span className="flex items-center gap-0.5">
                            <MapPin className="w-2.5 h-2.5" />
                            {item.place}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
