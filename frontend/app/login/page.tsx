"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { Mail, Lock, Eye, EyeOff } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { login } from "@/lib/api/auth"
import { setToken } from "@/lib/api/client"

export default function LoginPage() {
  const router = useRouter()
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!email || !password) {
      setError("이메일과 비밀번호를 입력해주세요")
      return
    }

    try {
      setLoading(true)
      setError("")
      const data = await login({ email, password })
      setToken(data.token)
      localStorage.setItem("userId", data.userId)
      if (data.refreshToken) {
        localStorage.setItem("refreshToken", data.refreshToken)
      }
      router.push("/")
    } catch (err: any) {
      console.error("로그인 실패:", err)
      setError("이메일 또는 비밀번호가 올바르지 않습니다")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="h-screen bg-muted flex justify-center overflow-hidden">
      <div className="w-full max-w-[430px] h-full bg-background flex flex-col relative shadow-xl">
        {/* Header */}
        <div className="flex-1 flex flex-col items-center justify-center px-8">
          {/* Logo */}
          <div className="mb-8 text-center">
            <h1 className="text-2xl font-bold text-foreground">일기예보 🌤️</h1>
            <p className="text-sm text-muted-foreground mt-1">
              일기로 예견하는 보석같은 만남
            </p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="w-full space-y-4">
            <div>
              <Label htmlFor="email" className="text-xs text-muted-foreground">
                이메일
              </Label>
              <div className="relative mt-1">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  id="email"
                  type="email"
                  placeholder="university@kookmin.ac.kr"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="pl-10 h-12 text-base"
                />
              </div>
            </div>

            <div>
              <Label htmlFor="password" className="text-xs text-muted-foreground">
                비밀번호
              </Label>
              <div className="relative mt-1">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  placeholder="비밀번호 입력"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="pl-10 pr-10 h-12 text-base"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            {error && (
              <p className="text-xs text-destructive text-center">{error}</p>
            )}

            <Button
              type="submit"
              disabled={loading}
              className="w-full h-12 text-base font-medium mt-2"
            >
              {loading ? "로그인 중..." : "로그인"}
            </Button>
          </form>

          {/* Links */}
          <div className="mt-6 text-center">
            <p className="text-xs text-muted-foreground">
              아직 계정이 없으신가요?{" "}
              <button
                onClick={() => router.push("/signup")}
                className="text-primary font-medium"
              >
                회원가입
              </button>
            </p>
          </div>
        </div>

        {/* Footer */}
        <div className="pb-8 text-center">
          <p className="text-[10px] text-muted-foreground">일기예보 v1.0.0</p>
        </div>
      </div>
    </div>
  )
}
