import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { getAlerts, cancelAlert, type Alert, type AlertStatus } from "../api/alerts";

const STATUS_LABEL: Record<AlertStatus, string> = {
  ACTIVE: "감시중",
  TRIGGERED: "도달",
  CANCELLED: "취소됨",
};

const STATUS_COLOR: Record<AlertStatus, string> = {
  ACTIVE: "var(--text-secondary)",
  TRIGGERED: "var(--color-up)",
  CANCELLED: "var(--text-tertiary)",
};

function fmtPrice(p: number | null): string {
  if (p == null) return "-";
  return Number.isInteger(p) ? p.toLocaleString() : p.toLocaleString(undefined, { maximumFractionDigits: 4 });
}

export default function AlertsPage() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ["alerts"], queryFn: getAlerts });

  const cancel = useMutation({
    mutationFn: (id: number) => cancelAlert(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["alerts"] });
      qc.invalidateQueries({ queryKey: ["alerts", "triggered-count"] });
    },
  });

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <section style={styles.head}>
        <h2 style={styles.title}>가격 알림</h2>
        <span style={styles.count}>{data?.length ?? 0}건</span>
      </section>

      <section style={styles.card}>
        <table style={styles.table} className="tabular">
          <thead>
            <tr style={styles.thRow}>
              <th style={styles.th}>종목</th>
              <th style={styles.th}>조건</th>
              <th style={styles.thNum}>목표가</th>
              <th style={styles.th}>상태</th>
              <th style={styles.thNum}>도달가</th>
              <th style={styles.thNum}></th>
            </tr>
          </thead>
          <tbody>
            {data?.map((a: Alert) => (
              <tr key={a.alertId} style={styles.tdRow}>
                <td style={styles.td}>
                  <Link to={`/stocks/${a.ticker}`} style={styles.link}>{a.ticker}</Link>
                </td>
                <td style={styles.td}>{a.direction === "ABOVE" ? "이상 ▲" : "이하 ▼"}</td>
                <td style={styles.tdNum}>{fmtPrice(a.targetPrice)}</td>
                <td style={{ ...styles.td, color: STATUS_COLOR[a.status], fontWeight: 600 }}>
                  {STATUS_LABEL[a.status]}
                </td>
                <td style={styles.tdNum}>{fmtPrice(a.triggeredPrice)}</td>
                <td style={styles.tdNum}>
                  {a.status !== "CANCELLED" && (
                    <button
                      onClick={() => cancel.mutate(a.alertId)}
                      disabled={cancel.isPending}
                      style={styles.cancelBtn}
                    >{a.status === "TRIGGERED" ? "확인" : "취소"}</button>
                  )}
                </td>
              </tr>
            ))}
            {(!data || data.length === 0) && (
              <tr>
                <td colSpan={6} style={styles.empty}>
                  {isLoading
                    ? "불러오는 중..."
                    : "설정한 알림이 없습니다. 종목 상세에서 목표가 알림을 추가하세요."}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </section>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  head: { display: "flex", alignItems: "baseline", gap: 10 },
  title: { fontSize: 18, fontWeight: 700 },
  count: { fontSize: 13, color: "var(--text-secondary)" },
  card: {
    background: "var(--bg-panel)", border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-lg)", padding: 12,
  },
  table: { width: "100%", borderCollapse: "collapse", fontSize: 13 },
  thRow: { borderBottom: "1px solid var(--border-subtle)" },
  th: { textAlign: "left" as const, padding: "10px 8px", color: "var(--text-secondary)", fontWeight: 600, fontSize: 12 },
  thNum: { textAlign: "right" as const, padding: "10px 8px", color: "var(--text-secondary)", fontWeight: 600, fontSize: 12 },
  tdRow: { borderBottom: "1px solid var(--border-subtle)" },
  td: { padding: "10px 8px" },
  tdNum: { padding: "10px 8px", textAlign: "right" as const },
  empty: { padding: 24, textAlign: "center" as const, color: "var(--text-tertiary)" },
  link: { color: "var(--text-primary)", fontWeight: 600 },
  cancelBtn: {
    height: 28, padding: "0 10px", fontSize: 12,
    background: "transparent", color: "var(--text-secondary)",
    border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)", cursor: "pointer",
  },
};
