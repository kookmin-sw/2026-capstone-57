import { apiFetch } from "./client"

export interface ProfileOptionDto {
  code: string
  label: string
}

export interface ProfileOptionsResponse {
  hobbies: ProfileOptionDto[]
  interests: ProfileOptionDto[]
  personalityTypes: ProfileOptionDto[]
  idealTypes: ProfileOptionDto[]
}

/** 프로필 옵션 조회 (취미, 관심사, 성격, 이상형 각 10개 랜덤) */
export function getProfileOptions() {
  return apiFetch<ProfileOptionsResponse>("/api/users/profile/options")
}
