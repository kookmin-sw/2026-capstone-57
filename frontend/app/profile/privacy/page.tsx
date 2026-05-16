"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { ArrowLeft, Lock, Eye, EyeOff } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent } from "@/components/ui/card"
import { Switch } from "@/components/ui/switch"

export default function PrivacyPage() {
  const router = useRouter()
  const [showPasswordChange, setShowPasswordChange] = useState(false)
  const [currentPassword, setCurrentPassword] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [showCurrent, setShowCurrent] = useState(false)
  const [showNew, setShowNew] = useState(false)
  const [error, setError] = useState("")

  const [profilePublic, setProfilePublic] = useState(true)
  const [showHobbies, setShowHobbies] = useState(true)
  const [showDepartment, setShowDepartment] = useState(true)

  const handlePasswordChange = () => {
    if (!currentPassword || !newPassword || !confirmPassword) {
      setError("모든 항목을 입력해주세요")
      return
    }
    if (newPassword.length < 8) {
      setError("새 비밀번호는 8자 이상이어야 합니다")
      return
    }
    if (newPassword !== confirmPassword) {
      setError("새 비밀번호가 일치하지 않습니다")
      return
    }
    // TODO: API 연동
    alert("비밀번호가 변경되었습니다")
    setShowPasswordChange(false)
    setCurrentPassword("")
    setNewPassword("")
    setConfirmPassword("")
    setError("")
  }

  const handleDeleteAccount = () => {
    if (confirm("정말 탈퇴하시겠습니까? 모든 데이터가 삭제됩니다.")) {
      // TODO: API 연동
      alert("탈퇴 처리되었습니다")
      localStorage.clear()
      router.push("/login")
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
          <h1 className="text-base font-semibold text-foreground">개인정보 및 보안</h1>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto px-5 py-5 space-y-5">
          {/* Password */}
          <Card className="border-0 shadow-sm">
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Lock className="w-4 h-4 text-muted-foreground" />
                  <span className="text-sm font-medium text-foreground">비밀번호 변경</span>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setShowPasswordChange(!showPasswordChange)}
                  className="h-7 text-xs"
                >
                  {showPasswordChange ? "취소" : "변경"}
                </Button>
              </div>

              {showPasswordChange && (
                <div className="mt-4 space-y-3">
                  <div>
                    <Label className="text-xs text-muted-foreground">현재 비밀번호</Label>
                    <div className="relative mt-1">
                      <Input
                        type={showCurrent ? "text" : "password"}
                        value={currentPassword}
                        onChange={(e) => setCurrentPassword(e.target.value)}
                        className="h-10 text-sm pr-10"
                      />
                      <button
                        type="button"
                        onClick={() => setShowCurrent(!showCurrent)}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground"
                      >
                        {showCurrent ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                  </div>
                  <div>
                    <Label className="text-xs text-muted-foreground">새 비밀번호</Label>
                    <div className="relative mt-1">
                      <Input
                        type={showNew ? "text" : "password"}
                        placeholder="8자 이상"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        className="h-10 text-sm pr-10"
                      />
                      <button
                        type="button"
                        onClick={() => setShowNew(!showNew)}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground"
                      >
                        {showNew ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                  </div>
                  <div>
                    <Label className="text-xs text-muted-foreground">새 비밀번호 확인</Label>
                    <Input
                      type="password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      className="mt-1 h-10 text-sm"
                    />
                  </div>
                  {error && <p className="text-xs text-destructive">{error}</p>}
                  <Button onClick={handlePasswordChange} className="w-full h-9 text-sm">
                    비밀번호 변경
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Privacy Settings */}
          <Card className="border-0 shadow-sm">
            <CardContent className="p-4 space-y-4">
              <h3 className="text-sm font-medium text-foreground">공개 설정</h3>

              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-foreground">프로필 공개</p>
                  <p className="text-[11px] text-muted-foreground">매칭 상대에게 프로필 표시</p>
                </div>
                <Switch checked={profilePublic} onCheckedChange={setProfilePublic} />
              </div>

              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-foreground">취미 공개</p>
                  <p className="text-[11px] text-muted-foreground">매칭 상대에게 취미 표시</p>
                </div>
                <Switch checked={showHobbies} onCheckedChange={setShowHobbies} />
              </div>

              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-foreground">학과 공개</p>
                  <p className="text-[11px] text-muted-foreground">매칭 상대에게 학과 표시</p>
                </div>
                <Switch checked={showDepartment} onCheckedChange={setShowDepartment} />
              </div>
            </CardContent>
          </Card>

          {/* Danger Zone */}
          <Card className="border-0 shadow-sm border-destructive/20">
            <CardContent className="p-4">
              <h3 className="text-sm font-medium text-destructive mb-3">계정 관리</h3>
              <Button
                variant="outline"
                onClick={handleDeleteAccount}
                className="w-full h-9 text-sm text-destructive border-destructive/30 hover:bg-destructive/5"
              >
                회원 탈퇴
              </Button>
              <p className="text-[10px] text-muted-foreground mt-2 text-center">
                탈퇴 시 모든 데이터가 영구 삭제됩니다
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
