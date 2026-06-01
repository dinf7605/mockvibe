import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import {
  getNotifications,
  markNotificationRead,
  markAllNotificationsRead,
  NOTIF_ICON,
  type AppNotification,
} from "../api/notifications";
import { EmptyState } from "../components/EmptyState";

function timeAgo(iso: string): string {
  const d = new Date(iso).getTime();
  const diff = Date.now() - d;
  const m = Math.floor(diff / 60000);
  if (m < 1) return "방금";
  if (m < 60) return `${m}분 전`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}시간 전`;
  return new Date(iso).toLocaleDateString("ko-KR");
}

export default function NotificationsPage() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ["notifications", "list"],
    queryFn: () => getNotifications(0, 30),
  });

  const readOne = useMutation({
    mutationFn: (id: number) => markNotificationRead(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["notifications"] }),
  });
  const readAll = useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["notifications"] }),
  });

  const items = data?.content ?? [];
  const hasUnread = items.some((n) => !n.read);

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <section style={styles.head}>
        <h2 style={styles.title}>알림함</h2>
        <div style={{ flex: 1 }} />
        {hasUnread && (
          <button className="btn" style={{ height: 32, fontSize: 12 }}
                  onClick={() => readAll.mutate()} disabled={readAll.isPending}>
            모두 읽음
          </button>
        )}
      </section>

      <section style={styles.card}>
        {items.length === 0 ? (
          isLoading ? (
            <div style={styles.empty}>불러오는 중...</div>
          ) : (
            <EmptyState icon="🔔" title="알림이 없습니다"
              desc="가격 알림 도달 · 지정가 체결 · AI 코멘트가 여기에 실시간으로 쌓입니다." />
          )
        ) : (
          <ul style={styles.list}>
            {items.map((n: AppNotification) => (
              <li
                key={n.notificationId}
                style={{ ...styles.item, ...(n.read ? {} : styles.unread) }}
                onClick={() => !n.read && readOne.mutate(n.notificationId)}
              >
                <span style={styles.ico}>{NOTIF_ICON[n.type]}</span>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={styles.itemTitle}>
                    {n.link ? <Link to={n.link} style={styles.link}>{n.title}</Link> : n.title}
                  </div>
                  {n.body && <div style={styles.body}>{n.body}</div>}
                  <div style={styles.time}>{timeAgo(n.createdAt)}</div>
                </div>
                {!n.read && <span style={styles.dot} aria-label="미확인" />}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  head: { display: "flex", alignItems: "center", gap: 10 },
  title: { fontSize: 18, fontWeight: 700 },
  card: {
    background: "var(--bg-panel)", border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-lg)", padding: 8,
  },
  list: { listStyle: "none", margin: 0, padding: 0, display: "flex", flexDirection: "column" },
  item: {
    display: "flex", alignItems: "flex-start", gap: 12, padding: "12px 12px",
    borderBottom: "1px solid var(--border-subtle)", cursor: "pointer",
  },
  unread: { background: "var(--bg-hover)" },
  ico: { fontSize: 18, lineHeight: 1.4 },
  itemTitle: { fontSize: 14, fontWeight: 600 },
  body: { fontSize: 13, color: "var(--text-secondary)", marginTop: 2, lineHeight: 1.5 },
  time: { fontSize: 11, color: "var(--text-tertiary)", marginTop: 4 },
  link: { color: "var(--text-primary)" },
  dot: { width: 8, height: 8, borderRadius: 999, background: "var(--color-up)", marginTop: 6, flexShrink: 0 },
  empty: { padding: 24, textAlign: "center" as const, color: "var(--text-tertiary)" },
};
