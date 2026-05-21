import { api } from "./client";
import type { AuthUser, Role } from "../store/authStore";

interface SignupRequest {
  username: string;
  email: string;
  password: string;
}

interface SignupResponse {
  userId: string;
  username: string;
  email: string;
  role: Role;
  seedMoneyKrw: number;
}

interface LoginRequest {
  email: string;
  password: string;
}

interface TokenResponse {
  accessToken: string;
  accessTokenExpiresInSeconds: number;
  userId: string;
  username: string;
  role: Role;
}

export async function signup(req: SignupRequest): Promise<SignupResponse> {
  const res = await api.post<SignupResponse>("/auth/signup", req);
  return res.data;
}

export async function login(req: LoginRequest): Promise<{ accessToken: string; user: AuthUser }> {
  const res = await api.post<TokenResponse>("/auth/login", req);
  return {
    accessToken: res.data.accessToken,
    user: {
      userId: res.data.userId,
      username: res.data.username,
      email: req.email,
      role: res.data.role,
    },
  };
}

/** 401 인터셉터가 호출. 쿠키의 RT로 새 AT 발급. 실패하면 throw. */
export async function refresh(): Promise<{ accessToken: string; user: AuthUser }> {
  const res = await api.post<TokenResponse>("/auth/refresh");
  return {
    accessToken: res.data.accessToken,
    user: {
      userId: res.data.userId,
      username: res.data.username,
      email: "",
      role: res.data.role,
    },
  };
}

export async function logout(): Promise<void> {
  try {
    await api.post("/auth/logout");
  } catch {
    /* 멱등 — 토큰 만료/누락이어도 클라이언트 측 상태는 어쨌든 정리 */
  }
}
