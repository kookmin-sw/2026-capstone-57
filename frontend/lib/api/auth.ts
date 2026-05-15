import { apiFetch } from "./client"

export interface LoginRequest {
  email: string
  password: string
}

export interface AuthTokenResponse {
  userId: string
  token: string
  refreshToken: string
}

export interface SignupRequest {
  verificationId: string
  password: string
  nickname: string
  name: string
  major: string
  studentId?: string
  birthDate: string
  gender: "MALE" | "FEMALE" | "OTHER"
  hobbies?: string[]
  interests?: string[]
  personalityTypes?: string[]
  idealTypes?: string[]
}

/** 로그인 */
export function login(data: LoginRequest) {
  return apiFetch<AuthTokenResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(data),
  })
}

/** 회원가입 */
export function signup(data: SignupRequest) {
  return apiFetch<AuthTokenResponse>("/api/auth/signup", {
    method: "POST",
    body: JSON.stringify(data),
  })
}

/** 인증 코드 발송 */
export function sendVerification(email: string) {
  return apiFetch<{ verificationId: string }>("/api/auth/verify/send", {
    method: "POST",
    body: JSON.stringify({ email }),
  })
}

/** 인증 코드 확인 */
export function confirmVerification(verificationId: string, code: string) {
  return apiFetch<{ verificationId: string; email: string; verified: boolean }>(
    "/api/auth/verify/confirm",
    { method: "POST", body: JSON.stringify({ verificationId, code }) }
  )
}
