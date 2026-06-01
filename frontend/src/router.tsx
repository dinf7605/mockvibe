import { lazy, Suspense, type ReactNode } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";
import AppLayout from "./layouts/AppLayout";
import { RequireAdmin } from "./components/RequireAdmin";
import { RequireAuth } from "./components/RequireAuth";

// 라우트별 코드 스플리팅 — 차트 라이브러리(ApexCharts/lightweight-charts)가 무거워
// 초기 번들에서 분리하고 페이지 진입 시 필요한 청크만 로드한다.
const LandingPage = lazy(() => import("./pages/LandingPage"));
const LoginPage = lazy(() => import("./pages/LoginPage"));
const SignupPage = lazy(() => import("./pages/SignupPage"));
const DashboardPage = lazy(() => import("./pages/DashboardPage"));
const StockSearchPage = lazy(() => import("./pages/StockSearchPage"));
const StockDetailPage = lazy(() => import("./pages/StockDetailPage"));
const WatchlistPage = lazy(() => import("./pages/WatchlistPage"));
const AlertsPage = lazy(() => import("./pages/AlertsPage"));
const HistoryPage = lazy(() => import("./pages/HistoryPage"));
const LimitOrdersPage = lazy(() => import("./pages/LimitOrdersPage"));
const BacktestPage = lazy(() => import("./pages/BacktestPage"));
const RiskPage = lazy(() => import("./pages/RiskPage"));
const RankingPage = lazy(() => import("./pages/RankingPage"));
const NotFoundPage = lazy(() => import("./pages/NotFoundPage"));

const AdminLayout = lazy(() => import("./pages/AdminPage"));
const AdminUsersPage = lazy(() => import("./pages/AdminPage").then((m) => ({ default: m.AdminUsersPage })));
const AdminStocksPage = lazy(() => import("./pages/AdminPage").then((m) => ({ default: m.AdminStocksPage })));
const AdminTradesPage = lazy(() => import("./pages/AdminPage").then((m) => ({ default: m.AdminTradesPage })));
const AdminSystemPage = lazy(() => import("./pages/AdminPage").then((m) => ({ default: m.AdminSystemPage })));
const AdminAiCostPage = lazy(() => import("./pages/AdminPage").then((m) => ({ default: m.AdminAiCostPage })));
const AdminAnnouncementsPage = lazy(() => import("./pages/AdminPage").then((m) => ({ default: m.AdminAnnouncementsPage })));
const AdminAuditPage = lazy(() => import("./pages/AdminPage").then((m) => ({ default: m.AdminAuditPage })));

/** 공개 페이지용 Suspense 래퍼 (인증 페이지는 AppLayout 내부 Suspense 가 처리). */
function Public({ children }: { children: ReactNode }) {
  return <Suspense fallback={<div style={{ minHeight: "100vh" }} />}>{children}</Suspense>;
}

/**
 * 라우팅 정책
 * - "/"        : 공개 랜딩 페이지 (비/로그인 모두 접근 가능)
 * - "/login"   : 공개
 * - "/signup"  : 공개
 * - "/dashboard"·"/search"·"/stocks/:ticker" 등 : RequireAuth (AppLayout 안)
 * - "/admin/*" : RequireAdmin (관리자만)
 */
export const router = createBrowserRouter([
  { path: "/", element: <Public><LandingPage /></Public> },
  { path: "/login", element: <Public><LoginPage /></Public> },
  { path: "/signup", element: <Public><SignupPage /></Public> },
  {
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { path: "dashboard", element: <DashboardPage /> },
      { path: "search", element: <StockSearchPage /> },
      { path: "watchlist", element: <WatchlistPage /> },
      { path: "alerts", element: <AlertsPage /> },
      { path: "stocks/:ticker", element: <StockDetailPage /> },
      { path: "history", element: <HistoryPage /> },
      { path: "orders", element: <LimitOrdersPage /> },
      { path: "backtest", element: <BacktestPage /> },
      { path: "risk", element: <RiskPage /> },
      { path: "ranking", element: <RankingPage /> },
      {
        path: "admin",
        element: (
          <RequireAdmin>
            <AdminLayout />
          </RequireAdmin>
        ),
        children: [
          { index: true, element: <Navigate to="users" replace /> },
          { path: "users", element: <AdminUsersPage /> },
          { path: "stocks", element: <AdminStocksPage /> },
          { path: "trades", element: <AdminTradesPage /> },
          { path: "system", element: <AdminSystemPage /> },
          { path: "ai-cost", element: <AdminAiCostPage /> },
          { path: "announcements", element: <AdminAnnouncementsPage /> },
          { path: "audit", element: <AdminAuditPage /> },
        ],
      },
    ],
  },
  { path: "*", element: <Public><NotFoundPage /></Public> },
]);
