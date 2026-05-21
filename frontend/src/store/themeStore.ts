import { create } from "zustand";
import { persist } from "zustand/middleware";

export type ThemeMode = "light" | "dark";

interface ThemeState {
  mode: ThemeMode;
  toggle: () => void;
  set: (mode: ThemeMode) => void;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set, get) => ({
      mode: "light",
      toggle: () => {
        const next: ThemeMode = get().mode === "light" ? "dark" : "light";
        document.documentElement.setAttribute("data-theme", next);
        set({ mode: next });
      },
      set: (mode) => {
        document.documentElement.setAttribute("data-theme", mode);
        set({ mode });
      },
    }),
    {
      name: "simulator-theme",
      onRehydrateStorage: () => (state) => {
        if (state) {
          document.documentElement.setAttribute("data-theme", state.mode);
        }
      },
    }
  )
);
