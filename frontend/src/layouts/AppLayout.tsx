import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useThemeStore } from "../store/themeStore";
import { useAuthStore } from "../store/authStore";
import { logout as apiLogout } from "../api/auth";
import { getTriggeredCount } from "../api/alerts";

interface NavItem {
  to: string;
  label: string;
  icon: string;
  end?: boolean;
  primary?: boolean; // 모바일 하단 탭바에 노출
  badge?: boolean; // 알림 미확인 배지
}

const NAV: NavItem[] = [
  { to: "/dashboard", label: "대시보드", icon: "📊", end: true, primary: true },
  { to: "/search", label: "종목 검색", icon: "🔍", primary: true },
  { to: "/watchlist", label: "관심종목", icon: "⭐", primary: true },
  { to: "/alerts", label: "알림", icon: "🔔", primary: true, badge: true },
  { to: "/history", label: "거래 내역", icon: "🧾" },
  { to: "/orders", label: "예약 주문", icon: "⏱️" },
  { to: "/backtest", label: "백테스트", icon: "📈" },
  { to: "/risk", label: "리스크", icon: "⚠️" },
];

export default function AppLayout() {
  const navigate = useNavigate();
  const mode = useThemeStore((s) => s.mode);
  const toggle = useThemeStore((s) => s.toggle);
  const isAdmin = useAuthStore((s) => s.isAdmin());
  const user = useAuthStore((s) => s.user);
  const clear = useAuthStore((s) => s.clear);
  const [moreOpen, setMoreOpen] = useState(false);

  // 알림 벨 배지 — 미확인(TRIGGERED) 개수 주기적 폴링
  const { data: triggeredCount } = useQuery({
    queryKey: ["alerts", "triggered-count"],
    queryFn: getTriggeredCount,
    refetchInterval: 15_000,
    staleTime: 10_000,
  });
  const badge = triggeredCount && triggeredCount > 0 ? triggeredCount : null;

  async function handleLogout() {
    await apiLogout();
    clear();
    navigate("/login", { replace: true });
  }

  const primaryItems = NAV.filter((n) => n.primary);
  const secondaryItems = NAV.filter((n) => !n.primary);

  function navClass({ isActive }: { isActive: boolean }) {
    return isActive ? "nav-link active" : "nav-link";
  }
  function tabClass({ isActive }: { isActive: boolean }) {
    return isActive ? "tab-link active" : "tab-link";
  }

  return (
    <div className="app-shell">
      {/* ===== 사이드바 (데스크톱) ===== */}
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span className="brand-dot" />
          fintech-simulator
        </div>

        {NAV.map((item) => (
          <NavLink key={item.to} to={item.to} end={item.end} className={navClass}>
            <span className="nav-ico">{item.icon}</span>
            {item.label}
            {item.badge && badge && <span className="nav-badge">{badge}</span>}
          </NavLink>
        ))}
        {isAdmin && (
          <NavLink to="/admin" className={({ isActive }) => `nav-link admin${isActive ? " active" : ""}`}>
            <span className="nav-ico">🛡️</span>
            관리자
          </NavLink>
        )}

        <div className="sidebar-spacer" />

        <div className="sidebar-foot">
          <button className="nav-link" onClick={toggle}>
            <span className="nav-ico">{mode === "dark" ? "☀️" : "🌙"}</span>
            {mode === "dark" ? "라이트 모드" : "다크 모드"}
          </button>
          {user && (
            <>
              <div className="sidebar-user">
                <span className="nav-ico">👤</span>
                {user.username}
              </div>
              <button className="nav-link" onClick={handleLogout}>
                <span className="nav-ico">↩</span>
                로그아웃
              </button>
            </>
          )}
        </div>
      </aside>

      {/* ===== 메인 ===== */}
      <div className="app-main">
        {/* 모바일 상단바 */}
        <header className="topbar-mobile">
          <div className="sidebar-brand" style={{ padding: 0 }}>
            <span className="brand-dot" />
            fintech-simulator
          </div>
          <button
            onClick={toggle}
            aria-label="테마 전환"
            style={{
              height: 34, width: 34, border: "1px solid var(--border-subtle)",
              borderRadius: "var(--radius-sm)", background: "transparent",
            }}
          >
            {mode === "dark" ? "☀️" : "🌙"}
          </button>
        </header>

        <main className="app-content">
          <Outlet />
        </main>
      </div>

      {/* ===== 모바일 하단 탭바 ===== */}
      <nav className="bottom-nav">
        {primaryItems.map((item) => (
          <NavLink key={item.to} to={item.to} end={item.end} className={tabClass}>
            <span className="tab-ico">{item.icon}</span>
            {item.label}
            {item.badge && badge && <span className="tab-badge">{badge}</span>}
          </NavLink>
        ))}
        <button className="tab-link" onClick={() => setMoreOpen(true)}>
          <span className="tab-ico">☰</span>
          더보기
        </button>
      </nav>

      {/* ===== 더보기 시트 (모바일) ===== */}
      {moreOpen && (
        <>
          <div className="more-backdrop" onClick={() => setMoreOpen(false)} />
          <div className="more-sheet">
            <div className="more-handle" />
            {secondaryItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={navClass}
                onClick={() => setMoreOpen(false)}
              >
                <span className="nav-ico">{item.icon}</span>
                {item.label}
              </NavLink>
            ))}
            {isAdmin && (
              <NavLink
                to="/admin"
                className="nav-link admin"
                onClick={() => setMoreOpen(false)}
              >
                <span className="nav-ico">🛡️</span>
                관리자
              </NavLink>
            )}
            <div style={{ height: 1, background: "var(--border-subtle)", margin: "6px 0" }} />
            {user && (
              <div className="sidebar-user">
                <span className="nav-ico">👤</span>
                {user.username}
              </div>
            )}
            <button
              className="nav-link"
              onClick={() => { setMoreOpen(false); handleLogout(); }}
            >
              <span className="nav-ico">↩</span>
              로그아웃
            </button>
          </div>
        </>
      )}
    </div>
  );
}
