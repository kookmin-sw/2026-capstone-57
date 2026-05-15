"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { ArrowLeft, RefreshCw } from "lucide-react"
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
import { getMyProfile, updateProfile } from "@/lib/api/user"
import { getProfileOptions, type ProfileOptionDto } from "@/lib/api/profile-options"
import { cn } from "@/lib/utils"

export default function ProfileEditPage() {
  const router = useRouter()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState("")

  // Basic info
  const [nickname, setNickname] = useState("")
  const [name, setName] = useState("")
  const [major, setMajor] = useState("")
  const [birthDate, setBirthDate] = useState("")
  const [gender, setGender] = useState<"MALE" | "FEMALE" | "OTHER">("MALE")

  // Options from server
  const [hobbyOptions, setHobbyOptions] = useState<ProfileOptionDto[]>([])
  const [interestOptions, setInterestOptions] = useState<ProfileOptionDto[]>([])
  const [personalityOptions, setPersonalityOptions] = useState<ProfileOptionDto[]>([])
  const [idealTypeOptions, setIdealTypeOptions] = useState<ProfileOptionDto[]>([])

  // Selected values
  const [selectedHobbies, setSelectedHobbies] = useState<string[]>([])
  const [selectedInterests, setSelectedInterests] = useState<string[]>([])
  const [selectedPersonality, setSelectedPersonality] = useState<string[]>([])
  const [selectedIdealTypes, setSelectedIdealTypes] = useState<string[]>([])

  useEffect(() => {
    async function load() {
      try {
        const [profile, options] = await Promise.all([
          getMyProfile(),
          getProfileOptions(),
        ])

        setNickname(profile.nickname || "")
        setName(profile.name || "")
        setMajor(profile.major || "")
        setBirthDate(profile.birthDate || "")
        setGender(profile.gender || "MALE")

        setSelectedHobbies(profile.hobbies || [])
        setSelectedInterests(profile.interests || [])
        setSelectedPersonality(profile.personalityTypes || [])
        setSelectedIdealTypes(profile.idealTypes || [])

        // 서버 옵션 + 기존 선택값 합치기 (기존 선택값이 옵션에 없을 수 있으므로)
        setHobbyOptions(mergeOptions(options.hobbies, profile.hobbies))
        setInterestOptions(mergeOptions(options.interests, profile.interests))
        setPersonalityOptions(mergeOptions(options.personalityTypes, profile.personalityTypes))
        setIdealTypeOptions(mergeOptions(options.idealTypes, profile.idealTypes))
      } catch (err) {
        console.error("프로필 로드 실패:", err)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  // 기존 선택값이 옵션 리스트에 없으면 추가 (label은 code를 한글화 시도)
  function mergeOptions(serverOptions: ProfileOptionDto[], existing: string[]): ProfileOptionDto[] {
    const codeToLabel = new Map(serverOptions.map((o) => [o.code, o.label]))
    const codes = new Set(serverOptions.map((o) => o.code))
    const extra = (existing || [])
      .filter((code) => !codes.has(code))
      .map((code) => ({ code, label: codeToLabel.get(code) || formatCode(code) }))
    return [...extra, ...serverOptions]
  }

  // CODE_LIKE_THIS → 보기 좋게 변환
  function formatCode(code: string): string {
    return code
      .replace(/_/g, " ")
      .replace(/\b\w/g, (c) => c.toUpperCase())
      .toLowerCase()
      .replace(/^\w/, (c) => c.toUpperCase())
  }

  const reloadOptions = async () => {
    try {
      const options = await getProfileOptions()
      setHobbyOptions(mergeOptions(options.hobbies, selectedHobbies))
      setInterestOptions(mergeOptions(options.interests, selectedInterests))
      setPersonalityOptions(mergeOptions(options.personalityTypes, selectedPersonality))
      setIdealTypeOptions(mergeOptions(options.idealTypes, selectedIdealTypes))
    } catch (err) {
      console.error("옵션 새로고침 실패:", err)
    }
  }

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

  const handleSave = async () => {
    if (!nickname || !name || !major || !birthDate) {
      setError("필수 항목을 모두 입력해주세요")
      return
    }
    if (selectedHobbies.length === 0 || selectedInterests.length === 0 ||
        selectedPersonality.length === 0 || selectedIdealTypes.length === 0) {
      setError("각 취향 항목에서 최소 1개 이상 선택해주세요")
      return
    }
    try {
      setSaving(true)
      setError("")
      await updateProfile({
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
      router.push("/profile")
    } catch (err) {
      console.error("프로필 수정 실패:", err)
      setError("수정에 실패했습니다")
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="h-screen bg-muted flex justify-center">
        <div className="w-full max-w-[430px] bg-background flex items-center justify-center">
          <p className="text-sm text-muted-foreground">로딩 중...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="h-screen bg-muted flex justify-center overflow-hidden">
      <div className="w-full max-w-[430px] h-full bg-background flex flex-col relative shadow-xl">
        {/* Header */}
        <div className="px-4 py-3 flex items-center justify-between shrink-0 border-b border-border/50">
          <div className="flex items-center gap-2">
            <button onClick={() => router.push("/profile")} className="p-1 text-muted-foreground hover:text-foreground">
              <ArrowLeft className="w-5 h-5" />
            </button>
            <h1 className="text-base font-semibold text-foreground">프로필 수정</h1>
          </div>
          <Button size="sm" onClick={handleSave} disabled={saving} className="h-8 text-xs px-4">
            {saving ? "저장 중..." : "저장"}
          </Button>
        </div>

        {/* Form */}
        <div className="flex-1 overflow-y-auto px-5 py-5 space-y-5">
          {/* Basic Info */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label className="text-xs text-muted-foreground">닉네임 *</Label>
              <Input value={nickname} onChange={(e) => setNickname(e.target.value)} className="mt-1 h-11 text-base" />
            </div>
            <div>
              <Label className="text-xs text-muted-foreground">이름 *</Label>
              <Input value={name} onChange={(e) => setName(e.target.value)} className="mt-1 h-11 text-base" />
            </div>
          </div>

          <div>
            <Label className="text-xs text-muted-foreground">학과 *</Label>
            <Input value={major} onChange={(e) => setMajor(e.target.value)} className="mt-1 h-11 text-base" />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label className="text-xs text-muted-foreground">생년월일 *</Label>
              <Input type="date" value={birthDate} onChange={(e) => setBirthDate(e.target.value)} className="mt-1 h-11 text-base" />
            </div>
            <div>
              <Label className="text-xs text-muted-foreground">성별 *</Label>
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

          {/* Divider */}
          <div className="border-t border-border/50 pt-4">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-semibold text-foreground">취향 설정</h3>
              <button onClick={reloadOptions} className="flex items-center gap-1 text-xs text-muted-foreground hover:text-primary">
                <RefreshCw className="w-3.5 h-3.5" />
                <span>새 옵션</span>
              </button>
            </div>

            {/* Hobbies */}
            <div className="mb-4">
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
              </div>
            </div>

            {/* Interests */}
            <div className="mb-4">
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
              </div>
            </div>

            {/* Personality */}
            <div className="mb-4">
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
              </div>
            </div>

            {/* Ideal Types */}
            <div className="mb-4">
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
              </div>
            </div>
          </div>

          {error && <p className="text-xs text-destructive">{error}</p>}
        </div>
      </div>
    </div>
  )
}
