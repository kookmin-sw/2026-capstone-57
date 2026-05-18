"use client"

import { useState, useEffect } from "react"
import {
  Settings,
  Bell,
  Shield,
  HelpCircle,
  LogOut,
  ChevronRight,
  Pencil,
  Trophy,
  Users,
  Calendar,
} from "lucide-react"
import { AppShell } from "@/components/app-shell"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { getMyProfile, type UserProfileDto } from "@/lib/api/user"
import { clearToken } from "@/lib/api/client"

interface MenuItemProps {
  icon: React.ReactNode
  label: string
  onClick?: () => void
  trailing?: React.ReactNode
  danger?: boolean
}

function MenuItem({ icon, label, onClick, trailing, danger }: MenuItemProps) {
  return (
    <button
      onClick={onClick}
      className={cn(
        "w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-colors",
        danger
          ? "text-destructive hover:bg-destructive/5"
          : "text-foreground hover:bg-muted/50"
      )}
    >
      <span className={cn("shrink-0", danger ? "text-destructive" : "text-muted-foreground")}>
        {icon}
      </span>
      <span className="flex-1 text-left text-sm">{label}</span>
      {trailing || <ChevronRight className="w-4 h-4 text-muted-foreground" />}
    </button>
  )
}

// 레벨별 필요 경험치 (간단한 계산)
function getExpProgress(totalExp: number, level: number): number {
  const expForCurrentLevel = level * 100
  const expInCurrentLevel = totalExp % expForCurrentLevel
  return Math.min(Math.round((expInCurrentLevel / expForCurrentLevel) * 100), 100)
}

export default function ProfilePage() {
  const [profile, setProfile] = useState<UserProfileDto | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function load() {
      try {
        const data = await getMyProfile()
        setProfile(data)
      } catch (err) {
        console.error("프로필 조회 실패:", err)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  const handleLogout = () => {
    clearToken()
    window.location.href = "/"
  }

  if (loading) {
    return (
      <AppShell title="MY">
        <div className="flex items-center justify-center h-40">
          <p className="text-sm text-muted-foreground">로딩 중...</p>
        </div>
      </AppShell>
    )
  }

  if (!profile) {
    return (
      <AppShell title="MY">
        <div className="flex flex-col items-center justify-center h-40 gap-2">
          <p className="text-sm text-muted-foreground">로그인이 필요합니다</p>
          <Button size="sm" onClick={() => window.location.href = "/login"}>
            로그인
          </Button>
        </div>
      </AppShell>
    )
  }

  const expProgress = getExpProgress(profile.totalExp, profile.currentLevel)

  return (
    <AppShell title="MY">
      <div className="px-4 py-4 space-y-4">
        {/* Profile Header */}
        <Card className="border-0 shadow-sm bg-gradient-to-br from-primary/5 to-primary/10">
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="w-14 h-14 rounded-2xl bg-primary/20 flex items-center justify-center ring-2 ring-primary/30">
                <span className="text-2xl">{profile.nickname.charAt(0)}</span>
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <h2 className="text-base font-bold text-foreground">{profile.nickname}</h2>
                  <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-primary/15 text-primary font-medium">
                    Lv.{profile.currentLevel}
                  </span>
                </div>
                <p className="text-xs text-muted-foreground mt-0.5">
                  {profile.major}
                </p>
                {/* Level progress */}
                <div className="flex items-center gap-2 mt-1.5">
                  <div className="flex-1 h-1.5 bg-muted rounded-full overflow-hidden">
                    <div
                      className="h-full bg-primary rounded-full transition-all"
                      style={{ width: `${expProgress}%` }}
                    />
                  </div>
                  <span className="text-[10px] text-muted-foreground">{expProgress}%</span>
                </div>
              </div>
              <Button variant="ghost" size="icon" className="shrink-0 w-8 h-8" onClick={() => window.location.href = "/profile/edit"}>
                <Pencil className="w-4 h-4 text-muted-foreground" />
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-2">
          <Card className="border-0 shadow-sm">
            <CardContent className="p-3 text-center">
              <div className="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center mx-auto mb-1">
                <Trophy className="w-4 h-4 text-primary" />
              </div>
              <p className="text-lg font-bold text-foreground">Lv.{profile.currentLevel}</p>
              <p className="text-[10px] text-muted-foreground">현재 레벨</p>
            </CardContent>
          </Card>
          <Card className="border-0 shadow-sm">
            <CardContent className="p-3 text-center">
              <div className="w-8 h-8 rounded-lg bg-accent/10 flex items-center justify-center mx-auto mb-1">
                <Users className="w-4 h-4 text-accent" />
              </div>
              <p className="text-lg font-bold text-foreground">{profile.totalExp}</p>
              <p className="text-[10px] text-muted-foreground">총 경험치</p>
            </CardContent>
          </Card>
          <Card className="border-0 shadow-sm">
            <CardContent className="p-3 text-center">
              <div className="w-8 h-8 rounded-lg bg-emerald-500/10 flex items-center justify-center mx-auto mb-1">
                <Calendar className="w-4 h-4 text-emerald-500" />
              </div>
              <p className="text-lg font-bold text-foreground">{profile.hobbies.length}</p>
              <p className="text-[10px] text-muted-foreground">취미</p>
            </CardContent>
          </Card>
        </div>

        {/* Tags */}
        {(profile.hobbies.length > 0 || profile.personalityType || profile.interests.length > 0 || profile.idealTypes.length > 0) && (
          <Card className="border-0 shadow-sm">
            <CardContent className="p-5 space-y-5">
              {profile.hobbies.length > 0 && (
                <div>
                  <h3 className="text-sm font-semibold text-foreground mb-2.5 flex items-center gap-1.5">
                    <span className="text-rose-300">♡</span> 내 취미
                  </h3>
                  <div className="flex flex-wrap gap-2">
                    {profile.hobbies.map((hobby) => (
                      <span
                        key={hobby.code}
                        className="px-3 py-1.5 rounded-full bg-[#FDE8EC] text-[#8B4558] text-xs font-medium"
                      >
                        #{hobby.label}
                      </span>
                    ))}
                  </div>
                </div>
              )}
              {profile.interests.length > 0 && (
                <div>
                  <h3 className="text-sm font-semibold text-foreground mb-2.5 flex items-center gap-1.5">
                    <span className="text-emerald-400">☆</span> 관심사
                  </h3>
                  <div className="flex flex-wrap gap-2">
                    {profile.interests.map((interest) => (
                      <span
                        key={interest.code}
                        className="px-3 py-1.5 rounded-full bg-[#E6F5EC] text-[#3D7A5A] text-xs font-medium"
                      >
                        #{interest.label}
                      </span>
                    ))}
                  </div>
                </div>
              )}
              {profile.idealTypes.length > 0 && (
                <div>
                  <h3 className="text-sm font-semibold text-foreground mb-2.5 flex items-center gap-1.5">
                    <span className="text-amber-400">♡</span> 이상형
                  </h3>
                  <div className="flex flex-wrap gap-2">
                    {profile.idealTypes.map((type) => (
                      <span
                        key={type.code}
                        className="px-3 py-1.5 rounded-full bg-[#FFF5E6] text-[#8A6D3B] text-xs font-medium"
                      >
                        #{type.label}
                      </span>
                    ))}
                  </div>
                </div>
              )}
              {profile.personalityType && (
                <div>
                  <h3 className="text-sm font-semibold text-foreground mb-2.5 flex items-center gap-1.5">
                    <span className="text-amber-300">✧</span> 성격 키워드
                  </h3>
                  <div className="flex flex-wrap gap-2">
                    <span className="px-3 py-1.5 rounded-full bg-[#EFF8E8] text-[#4A7A3D] text-xs font-medium">
                      {profile.personalityType.label}
                    </span>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        )}

        {/* Menu */}
        <Card className="border-0 shadow-sm">
          <CardContent className="p-2">
            <MenuItem
              icon={<Calendar className="w-4 h-4" />}
              label="시간표 등록"
              onClick={() => window.location.href = "/profile/schedule"}
            />
            <MenuItem icon={<Bell className="w-4 h-4" />} label="알림 설정" />
            <MenuItem icon={<Shield className="w-4 h-4" />} label="개인정보 및 보안" onClick={() => window.location.href = "/profile/privacy"} />
            <MenuItem icon={<Settings className="w-4 h-4" />} label="앱 설정" onClick={() => window.location.href = "/profile/settings"} />
            <MenuItem icon={<HelpCircle className="w-4 h-4" />} label="도움말 / 문의" />
            <div className="my-1 border-t border-border/50" />
            <MenuItem
              icon={<LogOut className="w-4 h-4" />}
              label="로그아웃"
              danger
              trailing={<span />}
              onClick={handleLogout}
            />
          </CardContent>
        </Card>

        <p className="text-center text-[10px] text-muted-foreground pb-2">
          일기예보 v1.0.0
        </p>
      </div>
    </AppShell>
  )
}
