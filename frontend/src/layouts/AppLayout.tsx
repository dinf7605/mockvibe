import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useThemeStore } from "../store/themeStore";
import { useAuthStore } from "../store/authStore";
import { logout as apiLogout } from "../api/auth";
import { getTriggeredCount } from "../api/alerts";

const NAV = [
  { to: "/dashboard", label: "대시보드", end: true },
  { to: "/search", label: "종목 검색" },
  { to: "/watchlist", label: "관심종목" },
  { to: "/alerts", label: "알림" },
  { to: "/history", label: "거래 내역" },
  { to: "/orders", label: "예약 주문" },
  { to: "/backtest", label: "백테스트" },
  { to: "/risk", label: "리스크" },
];

export default function AppLayout() {
  const navigate = useNavigate();
  const mode = useThemeStore((s) => s.mode);
  const toggle = useThemeStore((s) => s.toggle);
  const isAdmin = useAuthStore((s) => s.isAdmin());
  const user = useAuthStore((s) => s.user);
  const clear = useAuthStore((s) => s.clear);

  // 알림 벨 배지 — 미확인(TRIGGERED) 개수 주기적 폴링
  const { data: triggeredCount } = useQuery({
    queryKey: ["alerts", "triggered-count"],
    queryFn: getTriggeredCount,
    refetchInterval: 15_000,
    staleTime: 10_000,
  });

  async function handleLogout() {
    await apiLogout();
    clear();
    navigate("/login", { replace: true });
  }

  return (
    <div style={styles.shell}>
      <header style={styles.header}>
        <div style={styles.brand}>
          <span style={styles.brandDot} />
          fintech-simulator
        </div>

        <nav style={styles.nav}>
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              style={({ isActive }) => ({
                ...styles.navLink,
                color: isActive ? "var(--text-primary)" : "var(--text-secondary)",
                background: isActive ? "var(--bg-hover)" : "transparent",
              })}
            >
              {item.label}
              {item.to === "/alerts" && !!triggeredCount && triggeredCount > 0 && (
                <span style={styles.badge}>{triggeredCount}</span>
              )}
            </NavLink>
          ))}
          {isAdmin && (
            <NavLink
              to="/admin"
              style={({ isActive }) => ({
                ...styles.navLink,
                color: isActive ? "var(--color-primary)" : "var(--color-primary)",
                background: isActive ? "var(--bg-hover)" : "transparent",
                fontWeight: 600,
              })}
            >
              관리자
            </NavLink>
          )}
        </nav>

        <div style={styles.right}>
          <button onClick={toggle} style={styles.iconBtn} aria-label="테마 전환">
            {mode === "dark" ? "☀️" : "🌙"}
          </button>
          {user ? (
            <>
              <span style={styles.userName}>{user.username}</span>
              <button onClick={handleLogout} style={styles.iconBtn}>
                로그아웃
              </button>
            </>
          ) : (
            <NavLink to="/login" style={styles.iconBtn}>
              로그인
            </NavLink>
          )}
        </div>
      </header>

      <main style={styles.main}>
        <Outlet />
      </main>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  shell: {
    minHeight: "100vh",
    display: "flex",
    flexDirection: "column",
  },
  header: {
    height: 56,
    padding: "0 20px",
    display: "flex",
    alignItems: "center",
    gap: 24,
    borderBottom: "1px solid var(--border-subtle)",
    background: "var(--bg-panel)",
    position: "sticky",
    top: 0,
    zIndex: 10,
  },
  brand: {
    fontWeight: 700,
    fontSize: 15,
    display: "flex",
    alignItems: "center",
    gap: 8,
  },
  brandDot: {
    width: 8,
    height: 8,
    borderRadius: 999,
    background: "var(--color-up)",
    display: "inline-block",
  },
  nav: {
    display: "flex",
    gap: 4,
    flex: 1,
  },
  navLink: {
    padding: "6px 12px",
    borderRadius: "var(--radius-sm)",
    fontSize: 13,
    fontWeight: 500,
    transition: "all var(--duration-fast) var(--ease-emphasis)",
    display: "inline-flex",
    alignItems: "center",
    gap: 6,
  },
  badge: {
    minWidth: 16,
    height: 16,
    padding: "0 5px",
    borderRadius: 999,
    background: "var(--color-up)",
    color: "#fff",
    fontSize: 10,
    fontWeight: 700,
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    lineHeight: 1,
  },
  right: {
    display: "flex",
    alignItems: "center",
    gap: 8,
  },
  iconBtn: {
    height: 32,
    padding: "0 12px",
    background: "transparent",
    color: "var(--text-secondary)",
    border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-sm)",
    fontSize: 13,
    display: "inline-flex",
    alignItems: "center",
    gap: 6,
  },
  userName: {
    fontSize: 13,
    color: "var(--text-secondary)",
  },
  main: {
    flex: 1,
    padding: "24px 20px",
    maxWidth: 1280,
    width: "100%",
    margin: "0 auto",
  },
};
