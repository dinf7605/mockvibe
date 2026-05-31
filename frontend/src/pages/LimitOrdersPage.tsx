import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import {
  getLimitOrders,
  cancelLimitOrder,
  type LimitOrder,
  type LimitOrderStatus,
} from "../api/limitOrders";
import { useToast } from "../components/Toast";
import { EmptyState } from "../components/EmptyState";

const STATUS_LABEL: Record<LimitOrderStatus, string> = {
  PENDING: "대기중",
  FILLED: "체결됨",
  CANCELLED: "취소됨",
  EXPIRED: "만료",
};
const STATUS_COLOR: Record<LimitOrderStatus, string> = {
  PENDING: "var(--color-warning)",
  FILLED: "var(--color-success)",
  CANCELLED: "var(--text-tertiary)",
  EXPIRED: "var(--text-tertiary)",
};

function unitOf(ticker: string): string {
  return /^\d/.test(ticker) ? "₩" : "$";
}
function fmt(ticker: string, n: number): string {
  return `${unitOf(ticker)}${Number(n).toLocaleString(undefined, { maximumFractionDigits: 4 })}`;
}
function fmtDate(s: string | null): string {
  return s ? new Date(s).toLocaleDateString("ko-KR") : "-";
}

export default function LimitOrdersPage() {
  const qc = useQueryClient();
  const notify = useToast();
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["limitOrders", page],
    queryFn: () => getLimitOrders(page, 20),
    placeholderData: (prev) => prev,
  });

  const cancel = useMutation({
    mutationFn: (id: number) => cancelLimitOrder(id),
    onSuccess: () => {
      notify.success("지정가 주문을 취소했습니다.");
      qc.invalidateQueries({ queryKey: ["limitOrders"] });
    },
    onError: () => notify.error("취소에 실패했습니다."),
  });

  const items = data?.items ?? [];

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <section style={styles.head}>
        <h2 style={styles.title}>예약 주문 (지정가)</h2>
        <span style={styles.count}>{data?.totalElements ?? 0}건</span>
      </section>

      <section style={styles.card}>
        <div className="table-scroll">
          <table style={styles.table} className="tabular">
            <thead>
              <tr style={styles.thRow}>
                <th style={styles.th}>종목</th>
                <th style={styles.th}>구분</th>
                <th style={styles.thNum}>지정가</th>
                <th style={styles.thNum}>수량</th>
                <th style={styles.th}>상태</th>
                <th style={styles.th}>만료</th>
                <th style={styles.thNum}></th>
              </tr>
            </thead>
            <tbody>
              {items.map((o: LimitOrder) => (
                <tr key={o.limitOrderId} style={styles.tdRow}>
                  <td style={styles.td}>
                    <Link to={`/stocks/${o.ticker}`} style={styles.link}>{o.ticker}</Link>
                  </td>
                  <td style={styles.td} className={o.orderType === "BUY" ? "up" : "down"}>
                    {o.orderType === "BUY" ? "매수" : "매도"}
                  </td>
                  <td style={styles.tdNum}>{fmt(o.ticker, o.targetPrice)}</td>
                  <td style={styles.tdNum}>{o.quantity}</td>
                  <td style={{ ...styles.td, color: STATUS_COLOR[o.status], fontWeight: 600 }}>
                    {STATUS_LABEL[o.status]}
                  </td>
                  <td style={styles.td}>{fmtDate(o.expiresAt)}</td>
                  <td style={styles.tdNum}>
                    {o.status === "PENDING" && (
                      <button
                        onClick={() => cancel.mutate(o.limitOrderId)}
                        disabled={cancel.isPending}
                        style={styles.cancelBtn}
                      >취소</button>
                    )}
                  </td>
                </tr>
              ))}
              {items.length === 0 && (
                <tr><td colSpan={7}>
                  {isLoading ? (
                    <div style={styles.empty}>불러오는 중...</div>
                  ) : (
                    <EmptyState
                      icon="⏱️"
                      title="예약된 지정가 주문이 없습니다"
                      desc="종목 상세의 매매 패널에서 '지정가'를 선택해 목표가 주문을 걸어보세요."
                      to="/search"
                      ctaLabel="종목 검색하기"
                    />
                  )}
                </td></tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      {data && data.totalPages > 1 && (
        <div style={styles.pager}>
          <button onClick={() => setPage((p) => Math.max(p - 1, 0))} disabled={page === 0} style={styles.pageBtn}>이전</button>
          <span style={{ fontSize: 13, color: "var(--text-secondary)" }}>
            {data.page + 1} / {data.totalPages}
          </span>
          <button onClick={() => setPage((p) => p + 1)} disabled={page + 1 >= data.totalPages} style={styles.pageBtn}>다음</button>
        </div>
      )}
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
  pager: { display: "flex", gap: 12, alignItems: "center", justifyContent: "center" },
  pageBtn: {
    height: 32, padding: "0 14px", fontSize: 13,
    background: "var(--bg-panel)", color: "var(--text-primary)",
    border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)",
  },
};
