import { createBrowserRouter, Navigate } from "react-router-dom";
import AppLayout from "./layouts/AppLayout";
import { RequireAdmin } from "./components/RequireAdmin";
import { RequireAuth } from "./components/RequireAuth";
import LandingPage from "./pages/LandingPage";
import DashboardPage from "./pages/DashboardPage";
import LoginPage from "./pages/LoginPage";
import SignupPage from "./pages/SignupPage";
import StockSearchPage from "./pages/StockSearchPage";
import StockDetailPage from "./pages/StockDetailPage";
import HistoryPage from "./pages/HistoryPage";
import LimitOrdersPage from "./pages/LimitOrdersPage";
import BacktestPage from "./pages/BacktestPage";
import RiskPage from "./pages/RiskPage";
import AdminLayout, {
  AdminUsersPage,
  AdminStocksPage,
  AdminTradesPage,
  AdminSystemPage,
  AdminAiCostPage,
  AdminAnnouncementsPage,
  AdminAuditPage,
} from "./pages/AdminPage";
import NotFoundPage from "./pages/NotFoundPage";

/**
 * 라우팅 정책
 * - "/"        : 공개 랜딩 페이지 (비/로그인 모두 접근 가능)
 * - "/login"   : 공개
 * - "/signup"  : 공개
 * - "/dashboard"·"/search"·"/stocks/:ticker" 등 : RequireAuth (AppLayout 안)
 * - "/admin/*" : RequireAdmin (관리자만)
 */
export const router = createBrowserRouter([
  { path: "/", element: <LandingPage /> },
  { path: "/login", element: <LoginPage /> },
  { path: "/signup", element: <SignupPage /> },
  {
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { path: "dashboard", element: <DashboardPage /> },
      { path: "search", element: <StockSearchPage /> },
      { path: "stocks/:ticker", element: <StockDetailPage /> },
      { path: "history", element: <HistoryPage /> },
      { path: "orders", element: <LimitOrdersPage /> },
      { path: "backtest", element: <BacktestPage /> },
      { path: "risk", element: <RiskPage /> },
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
  { path: "*", element: <NotFoundPage /> },
]);
