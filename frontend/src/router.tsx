import { createBrowserRouter, Navigate } from "react-router-dom";
import AppLayout from "./layouts/AppLayout";
import { RequireAdmin } from "./components/RequireAdmin";
import { RequireAuth } from "./components/RequireAuth";
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

export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  { path: "/signup", element: <SignupPage /> },
  {
    path: "/",
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <DashboardPage /> },
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
      { path: "*", element: <NotFoundPage /> },
    ],
  },
]);
