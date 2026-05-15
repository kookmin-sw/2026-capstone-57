"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { ArrowLeft, Mail, Lock, User, GraduationCap, Eye, EyeOff, RefreshCw } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { sendVerification, confirmVerification, signup } from "@/lib/api/auth"
import { getProfileOptions, type ProfileOptionDto } from "@/lib/api/profile-options"
import { setToken } from "@/lib/api/client"
import { cn } from "@/lib/utils"

type Step = "email" | "verify" | "info" | "preferences"

export default function SignupPage() {
  const router = useRouter()
  const [step, setStep] = useState<Step>("email")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")

  // Step 1: Email
  const [email, setEmail] = useState("")
  const [verificationId, setVerificationId] = useState("")

  // Step 2: Verify
  const [code, setCode] = useState("")

  // Step 3: Info
  const [password, setPassword] = useState("")
  const [showPassword, setShowPassword] = useState(false)
  const [nickname, setNickname] = useState("")
  const [name, setName] = useState("")
  const [major, setMajor] = useState("")
  const [birthDate, setBirthDate] = useState("")
  const [gender, setGender] = useState<"MALE" | "FEMALE" | "OTHER">("MALE")

  // Step 4: Preferences
  const [hobbyOptions, setHobbyOptions] = useState<ProfileOptionDto[]>([])
  const [interestOptions, setInterestOptions] = useState<ProfileOptionDto[]>([])
  const [personalityOptions, setPersonalityOptions] = useState<ProfileOptionDto[]>([])
  const [idealTypeOptions, setIdealTypeOptions] = useState<ProfileOptionDto[]>([])

  const [selectedHobbies, setSelectedHobbies] = useState<string[]>([])
  const [selectedInterests, setSelectedInterests] = useState<string[]>([])
  const [selectedPersonality, setSelectedPersonality] = useState<string[]>([])
  const [selectedIdealTypes, setSelectedIdealTypes] = useState<string[]>([])

  // 프로필 옵션 로드
  const loadOptions = async () => {
    try {
      const options = await getProfileOptions()
      setHobbyOptions(options.hobbies)
      setInterestOptions(options.interests)
      setPersonalityOptions(options.personalityTypes)
      setIdealTypeOptions(options.idealTypes)
    } catch (err) {
      console.error("옵션 로드 실패:", err)
    }
  }

  useEffect(() => {
    if (step === "preferences") {
      loadOptions()
    }
  }, [step])

  // Toggle selection
  const toggleSelection = (
    item: string,
    selected: string[],
    setSelected: (v: string[]) => void,
    max: number = 5
  ) => {
    if (selected.includes(item)) {
      setSelected(selected.filter((s) => s !== item))
    } else if (selected.length < max) {
      setSelected([...selected, item])
    }
  }

  // 선택된 항목을 맨 앞에 정렬
  function sortOptions(options: ProfileOptionDto[], selected: string[]): ProfileOptionDto[] {
    return [...options].sort((a, b) => {
      const aSelected = selected.includes(a.code) ? 0 : 1
      const bSelected = selected.includes(b.code) ? 0 : 1
      return aSelected - bSelected
    })
  }

  // Step 1
  const handleSendCode = async () => {
    if (!email) { setError("이메일을 입력해주세요"); return }
    try {
      setLoading(true); setError("")
      const res = await sendVerification(email)
      setVerificationId(res.verificationId)
      setStep("verify")
    } catch { setError("인증 코드 발송에 실패했습니다") }
    finally { setLoading(false) }
  }

  // Step 2
  const handleVerifyCode = async () => {
    if (!code) { setError("인증 코드를 입력해주세요"); return }
    try {
      setLoading(true); setError("")
      const res = await confirmVerification(verificationId, code)
      if (res.verified) { setStep("info") }
      else { setError("인증 코드가 올바르지 않습니다") }
    } catch { setError("인증 확인에 실패했습니다") }
    finally { setLoading(false) }
  }

  // Step 3 → Step 4
  const handleInfoNext = () => {
    if (!password || !nickname || !name || !major || !birthDate) {
      setError("모든 필수 항목을 입력해주세요"); return
    }
    if (password.length < 8) {
      setError("비밀번호는 8자 이상이어야 합니다"); return
    }
    setError("")
    setStep("preferences")
  }

  // Step 4: 회원가입 완료
  const handleSignup = async () => {
    if (selectedHobbies.length === 0 || selectedInterests.length === 0 ||
        selectedPersonality.length === 0 || selectedIdealTypes.length === 0) {
      setError("각 항목에서 최소 1개 이상 선택해주세요"); return
    }
    try {
      setLoading(true); setError("")
      const res = await signup({
        verificationId,
        password,
        nickname,
        name,
        major,
        birthDate,
        gender,
        hobbies: selectedHobbies,
        interests: selectedInterests,
        personalityTypes: selectedPersonality,
        idealTypes: selectedIdealTypes,
      })
      setToken(res.token)
      localStorage.setItem("userId", res.userId)
      router.push("/")
    } catch { setError("회원가입에 실패했습니다") }
    finally { setLoading(false) }
  }

  const goBack = () => {
    if (step === "email") router.push("/login")
    else if (step === "verify") setStep("email")
    else if (step === "info") setStep("verify")
    else setStep("info")
    setError("")
  }

  return (
    <div className="h-screen bg-muted flex justify-center overflow-hidden">
      <div className="w-full max-w-[430px] h-full bg-background flex flex-col relative shadow-xl">
        {/* Header */}
        <div className="px-4 py-3 flex items-center gap-2 shrink-0 border-b border-border/50">
          <button onClick={goBack} className="p-1 text-muted-foreground hover:text-foreground">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <h1 className="text-base font-semibold text-foreground">회원가입</h1>
        </div>

        {/* Progress */}
        <div className="px-4 pt-4 shrink-0">
          <div className="flex gap-1">
            <div className={cn("h-1 flex-1 rounded-full", ["email","verify","info","preferences"].indexOf(step) >= 0 ? "bg-primary" : "bg-muted")} />
            <div className={cn("h-1 flex-1 rounded-full", ["verify","info","preferences"].indexOf(step) >= 0 ? "bg-primary" : "bg-muted")} />
            <div className={cn("h-1 flex-1 rounded-full", ["info","preferences"].indexOf(step) >= 0 ? "bg-primary" : "bg-muted")} />
            <div className={cn("h-1 flex-1 rounded-full", step === "preferences" ? "bg-primary" : "bg-muted")} />
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 px-6 py-6 overflow-y-auto">
          {/* Step 1: Email */}
          {step === "email" && (
            <div className="space-y-6">
              <div>
                <h2 className="text-lg font-bold text-foreground">대학 이메일 인증</h2>
                <p className="text-sm text-muted-foreground mt-1">학교 이메일로 인증 코드를 보내드려요</p>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">이메일</Label>
                <div className="relative mt-1">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                  <Input type="email" placeholder="example@kookmin.ac.kr" value={email} onChange={(e) => setEmail(e.target.value)} className="pl-10 h-12 text-base" />
                </div>
              </div>
              {error && <p className="text-xs text-destructive">{error}</p>}
              <Button onClick={handleSendCode} disabled={loading} className="w-full h-12 text-base font-medium">
                {loading ? "발송 중..." : "인증 코드 받기"}
              </Button>
            </div>
          )}

          {/* Step 2: Verify */}
          {step === "verify" && (
            <div className="space-y-6">
              <div>
                <h2 className="text-lg font-bold text-foreground">인증 코드 입력</h2>
                <p className="text-sm text-muted-foreground mt-1">
                  <span className="text-primary font-medium">{email}</span>으로 보낸 6자리 코드
                </p>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">인증 코드</Label>
                <Input type="text" placeholder="6자리 코드" value={code} onChange={(e) => setCode(e.target.value)} maxLength={6} className="mt-1 h-12 text-center text-xl tracking-widest font-mono" />
              </div>
              {error && <p className="text-xs text-destructive">{error}</p>}
              <Button onClick={handleVerifyCode} disabled={loading || code.length < 6} className="w-full h-12 text-base font-medium">
                {loading ? "확인 중..." : "인증 확인"}
              </Button>
              <button onClick={handleSendCode} className="w-full text-center text-xs text-muted-foreground hover:text-primary">
                코드를 못 받으셨나요? 다시 보내기
              </button>
            </div>
          )}

          {/* Step 3: Info */}
          {step === "info" && (
            <div className="space-y-5">
              <div>
                <h2 className="text-lg font-bold text-foreground">기본 정보</h2>
                <p className="text-sm text-muted-foreground mt-1">프로필 정보를 입력해주세요</p>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">비밀번호</Label>
                <div className="relative mt-1">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                  <Input type={showPassword ? "text" : "password"} placeholder="8자 이상" value={password} onChange={(e) => setPassword(e.target.value)} className="pl-10 pr-10 h-11 text-base" />
                  <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground">
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <Label className="text-xs text-muted-foreground">이름</Label>
                  <div className="relative mt-1">
                    <User className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                    <Input placeholder="홍길동" value={name} onChange={(e) => setName(e.target.value)} className="pl-10 h-11 text-base" />
                  </div>
                </div>
                <div>
                  <Label className="text-xs text-muted-foreground">닉네임</Label>
                  <Input placeholder="별빛산책" value={nickname} onChange={(e) => setNickname(e.target.value)} className="mt-1 h-11 text-base" />
                </div>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">학과</Label>
                <div className="relative mt-1">
                  <GraduationCap className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                  <Input placeholder="컴퓨터공학과" value={major} onChange={(e) => setMajor(e.target.value)} className="pl-10 h-11 text-base" />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <Label className="text-xs text-muted-foreground">생년월일</Label>
                  <Input type="date" value={birthDate} onChange={(e) => setBirthDate(e.target.value)} className="mt-1 h-11 text-base" />
                </div>
                <div>
                  <Label className="text-xs text-muted-foreground">성별</Label>
                  <Select value={gender} onValueChange={(v) => setGender(v as any)}>
                    <SelectTrigger className="mt-1 h-11 text-base w-full"><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="MALE">남성</SelectItem>
                      <SelectItem value="FEMALE">여성</SelectItem>
                      <SelectItem value="OTHER">기타</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              {error && <p className="text-xs text-destructive">{error}</p>}
              <Button onClick={handleInfoNext} className="w-full h-12 text-base font-medium">다음</Button>
            </div>
          )}

          {/* Step 4: Preferences */}
          {step === "preferences" && (
            <div className="space-y-6">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-lg font-bold text-foreground">취향 선택</h2>
                  <p className="text-sm text-muted-foreground mt-1">각 항목에서 최소 1개 이상 선택해주세요</p>
                </div>
                <button onClick={loadOptions} className="p-2 text-muted-foreground hover:text-primary rounded-lg hover:bg-muted">
                  <RefreshCw className="w-4 h-4" />
                </button>
              </div>

              {/* Hobbies */}
              <div>
                <Label className="text-xs text-muted-foreground">취미 (최대 5개)</Label>
                <div className="flex flex-wrap gap-2 mt-2">
                  {sortOptions(hobbyOptions, selectedHobbies).map((opt) => (
                    <button
                      key={opt.code}
                      onClick={() => toggleSelection(opt.code, selectedHobbies, setSelectedHobbies)}
                      className={cn(
                        "px-3 py-1.5 rounded-full text-xs border transition-colors",
                        selectedHobbies.includes(opt.code)
                          ? "border-primary bg-primary/10 text-primary font-medium"
                          : "border-border text-muted-foreground hover:bg-muted"
                      )}
                    >
                      {opt.label}
                    </button>
                  ))}
                  {hobbyOptions.length === 0 && <p className="text-xs text-muted-foreground">로딩 중...</p>}
                </div>
              </div>

              {/* Interests */}
              <div>
                <Label className="text-xs text-muted-foreground">관심사 (최대 5개)</Label>
                <div className="flex flex-wrap gap-2 mt-2">
                  {sortOptions(interestOptions, selectedInterests).map((opt) => (
                    <button
                      key={opt.code}
                      onClick={() => toggleSelection(opt.code, selectedInterests, setSelectedInterests)}
                      className={cn(
                        "px-3 py-1.5 rounded-full text-xs border transition-colors",
                        selectedInterests.includes(opt.code)
                          ? "border-primary bg-primary/10 text-primary font-medium"
                          : "border-border text-muted-foreground hover:bg-muted"
                      )}
                    >
                      {opt.label}
                    </button>
                  ))}
                  {interestOptions.length === 0 && <p className="text-xs text-muted-foreground">로딩 중...</p>}
                </div>
              </div>

              {/* Personality */}
              <div>
                <Label className="text-xs text-muted-foreground">성격 (최대 5개)</Label>
                <div className="flex flex-wrap gap-2 mt-2">
                  {sortOptions(personalityOptions, selectedPersonality).map((opt) => (
                    <button
                      key={opt.code}
                      onClick={() => toggleSelection(opt.code, selectedPersonality, setSelectedPersonality)}
                      className={cn(
                        "px-3 py-1.5 rounded-full text-xs border transition-colors",
                        selectedPersonality.includes(opt.code)
                          ? "border-primary bg-primary/10 text-primary font-medium"
                          : "border-border text-muted-foreground hover:bg-muted"
                      )}
                    >
                      {opt.label}
                    </button>
                  ))}
                  {personalityOptions.length === 0 && <p className="text-xs text-muted-foreground">로딩 중...</p>}
                </div>
              </div>

              {/* Ideal Types */}
              <div>
                <Label className="text-xs text-muted-foreground">이상형 (최대 5개)</Label>
                <div className="flex flex-wrap gap-2 mt-2">
                  {sortOptions(idealTypeOptions, selectedIdealTypes).map((opt) => (
                    <button
                      key={opt.code}
                      onClick={() => toggleSelection(opt.code, selectedIdealTypes, setSelectedIdealTypes)}
                      className={cn(
                        "px-3 py-1.5 rounded-full text-xs border transition-colors",
                        selectedIdealTypes.includes(opt.code)
                          ? "border-primary bg-primary/10 text-primary font-medium"
                          : "border-border text-muted-foreground hover:bg-muted"
                      )}
                    >
                      {opt.label}
                    </button>
                  ))}
                  {idealTypeOptions.length === 0 && <p className="text-xs text-muted-foreground">로딩 중...</p>}
                </div>
              </div>

              {error && <p className="text-xs text-destructive">{error}</p>}

              <Button onClick={handleSignup} disabled={loading} className="w-full h-12 text-base font-medium">
                {loading ? "가입 중..." : "가입 완료"}
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
