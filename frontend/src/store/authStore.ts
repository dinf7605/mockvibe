import { create } from "zustand";
import { persist } from "zustand/middleware";

export type Role = "USER" | "ADMIN";

export interface AuthUser {
  userId: string;
  username: string;
  email: string;
  role: Role;
}

interface AuthState {
  accessToken: string | null;
  user: AuthUser | null;
  setAuth: (payload: { accessToken: string; user: AuthUser }) => void;
  clear: () => void;
  isAuthenticated: () => boolean;
  isAdmin: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      user: null,
      setAuth: ({ accessToken, user }) => set({ accessToken, user }),
      clear: () => set({ accessToken: null, user: null }),
      isAuthenticated: () => !!get().accessToken,
      isAdmin: () => get().user?.role === "ADMIN",
    }),
    {
      name: "simulator-auth",
      // Refresh Token은 백엔드가 httpOnly 쿠키로 관리, AccessToken만 클라 보관
      partialize: (s) => ({ accessToken: s.accessToken, user: s.user }),
    }
  )
);
