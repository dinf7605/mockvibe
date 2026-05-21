import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from "axios";
import { useAuthStore } from "../store/authStore";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 10_000,
  withCredentials: true, // refresh_token httpOnly 쿠키 전송
});

// ===== Request: Bearer 자동 부착 =====
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

// ===== Response: 401 → /auth/refresh 1회 자동 시도 =====
// 동시 다발 401을 단일 refresh promise로 큐잉.
type RetryConfig = AxiosRequestConfig & { _retry?: boolean };

let refreshPromise: Promise<string | null> | null = null;

async function tryRefresh(): Promise<string | null> {
  try {
    // auth.ts의 refresh()를 직접 import하면 순환 의존이라 fetch 형태로 호출
    const res = await axios.post(
      `${BASE_URL}/auth/refresh`,
      {},
      { withCredentials: true }
    );
    const data = res.data as {
      accessToken: string;
      userId: string;
      username: string;
      role: "USER" | "ADMIN";
    };
    useAuthStore.getState().setAuth({
      accessToken: data.accessToken,
      user: {
        userId: data.userId,
        username: data.username,
        email: useAuthStore.getState().user?.email ?? "",
        role: data.role,
      },
    });
    return data.accessToken;
  } catch {
    useAuthStore.getState().clear();
    return null;
  }
}

api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as RetryConfig | undefined;
    const status = error.response?.status;
    const url = original?.url ?? "";

    // /auth/* 자체 호출은 인터셉터 건드리지 않음 (무한 루프 방지)
    const isAuthCall = url.startsWith("/auth/");
    if (status !== 401 || !original || original._retry || isAuthCall) {
      return Promise.reject(error);
    }

    original._retry = true;
    refreshPromise = refreshPromise ?? tryRefresh().finally(() => {
      refreshPromise = null;
    });
    const newToken = await refreshPromise;
    if (!newToken) return Promise.reject(error);

    // 새 토큰으로 원요청 재시도
    original.headers = original.headers ?? {};
    (original.headers as Record<string, string>).Authorization = `Bearer ${newToken}`;
    return api.request(original);
  }
);
