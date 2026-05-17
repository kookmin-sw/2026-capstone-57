import { apiFetch } from "./client"
import type { ProfileOptionDto } from "./profile-options"

export type Gender = "MALE" | "FEMALE" | "OTHER"

export interface UserProfileDto {
  id: string
  email: string
  nickname: string
  name: string
  major: string
  birthDate: string
  gender: Gender
  hobbies: ProfileOptionDto[]
  interests: ProfileOptionDto[]
  personalityType: ProfileOptionDto | null
  idealTypes: ProfileOptionDto[]
  totalExp: number
  currentLevel: number
}

export interface ProfileSetup {
  nickname: string
  name: string
  major: string
  birthDate: string
  gender: Gender
  hobbies?: string[]
  interests?: string[]
  personalityType?: string
  idealTypes?: string[]
}

/** 내 프로필 조회 */
export function getMyProfile() {
  return apiFetch<UserProfileDto>("/api/users/me")
}

/** 프로필 수정 */
export function updateProfile(data: ProfileSetup) {
  return apiFetch<UserProfileDto>("/api/users/profile", {
    method: "PUT",
    body: JSON.stringify(data),
  })
}
