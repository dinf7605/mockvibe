import { NavLink, Outlet } from "react-router-dom";

const ADMIN_NAV = [
  { to: "users", label: "사용자" },
  { to: "stocks", label: "종목" },
  { to: "trades", label: "거래 모니터링" },
  { to: "system", label: "시스템" },
  { to: "ai-cost", label: "AI 비용" },
  { to: "announcements", label: "공지" },
  { to: "audit", label: "감사 로그" },
];

export default function AdminLayout() {
  return (
    <div style={{ display: "grid", gridTemplateColumns: "200px 1fr", gap: 24 }}>
      <aside
        style={{
          background: "var(--bg-panel)",
          border: "1px solid var(--border-subtle)",
          borderRadius: "var(--radius-lg)",
          padding: 12,
          height: "fit-content",
          position: "sticky",
          top: 80,
        }}
      >
        <div
          style={{
            fontSize: 11,
            fontWeight: 700,
            color: "var(--text-tertiary)",
            padding: "8px 12px",
            letterSpacing: 1,
          }}
        >
          ADMIN
        </div>
        {ADMIN_NAV.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            style={({ isActive }) => ({
              display: "block",
              padding: "8px 12px",
              borderRadius: "var(--radius-sm)",
              fontSize: 13,
              fontWeight: 500,
              color: isActive ? "var(--color-primary)" : "var(--text-secondary)",
              background: isActive ? "var(--bg-hover)" : "transparent",
              marginBottom: 2,
            })}
          >
            {item.label}
          </NavLink>
        ))}
      </aside>

      <div>
        <Outlet />
      </div>
    </div>
  );
}

export { AdminUsersPage } from "./admin/AdminUsersPage";
export { AdminStocksPage } from "./admin/AdminStocksPage";
export { AdminTradesPage } from "./admin/AdminTradesPage";
export { AdminSystemPage } from "./admin/AdminSystemPage";
export { AdminAiCostPage } from "./admin/AdminAiCostPage";
export { AdminAnnouncementsPage } from "./admin/AdminAnnouncementsPage";
export { AdminAuditPage } from "./admin/AdminAuditPage";
