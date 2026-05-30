import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { getHistory, type OrderSide } from "../api/trades";
import { EmptyState } from "../components/EmptyState";
import { formatKrw } from "../lib/format";

const SIDE_FILTERS: Array<{ label: string; value: OrderSide | "" }> = [
  { label: "전체", value: "" },
  { label: "매수", value: "BUY" },
  { label: "매도", value: "SELL" },
];

export default function HistoryPage() {
  const [side, setSide] = useState<OrderSide | "">("");
  const [page, setPage] = useState(0);

  const { data, isFetching } = useQuery({
    queryKey: ["history", page],
    queryFn: () => getHistory(page, 20),
    placeholderData: (prev) => prev,
  });

  const items = data?.items.filter((i) => !side || i.orderType === side) ?? [];

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <section style={styles.toolbar}>
        <div style={styles.tabs}>
          {SIDE_FILTERS.map((s) => (
            <button
              key={s.value}
              onClick={() => { setSide(s.value); setPage(0); }}
              style={{ ...styles.tab, ...(side === s.value ? styles.tabActive : {}) }}
            >{s.label}</button>
          ))}
        </div>
      </section>

      <section style={styles.card}>
        <div className="table-scroll">
        <table style={styles.table} className="tabular">
          <thead>
            <tr style={styles.thRow}>
              <th style={styles.th}>일시</th>
              <th style={styles.th}>종목</th>
              <th style={styles.th}>구분</th>
              <th style={styles.thNum}>가격</th>
              <th style={styles.thNum}>수량</th>
              <th style={styles.thNum}>수수료</th>
              <th style={styles.thNum}>총액(KRW)</th>
            </tr>
          </thead>
          <tbody>
            {items.map((it) => (
              <tr key={it.orderId} style={styles.tdRow}>
                <td style={styles.td}>{new Date(it.createdAt).toLocaleString("ko-KR")}</td>
                <td style={styles.td}>
                  <Link to={`/stocks/${it.ticker}`} style={styles.link}>{it.ticker}</Link>
                </td>
                <td style={styles.td} className={it.orderType === "BUY" ? "up" : "down"}>
                  {it.orderType === "BUY" ? "매수" : "매도"} · {it.orderMethod === "MARKET" ? "시장가" : "지정가"}
                </td>
                <td style={styles.tdNum}>{Number(it.price).toLocaleString("ko-KR")}</td>
                <td style={styles.tdNum}>{it.quantity}</td>
                <td style={styles.tdNum}>{formatKrw(it.fee)}</td>
                <td style={styles.tdNum}>{formatKrw(it.totalAmountKrw)}</td>
              </tr>
            ))}
            {items.length === 0 && (
              <tr><td colSpan={7}>
                {isFetching ? (
                  <div style={styles.empty}>불러오는 중...</div>
                ) : (
                  <EmptyState
                    icon="🧾"
                    title="거래 내역이 없습니다"
                    desc="첫 매수를 하면 여기에 체결 기록이 쌓입니다."
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
            {data.page + 1} / {data.totalPages} ({data.totalElements}건)
          </span>
          <button onClick={() => setPage((p) => p + 1)} disabled={page + 1 >= data.totalPages} style={styles.pageBtn}>다음</button>
        </div>
      )}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  toolbar: { display: "flex", gap: 12 },
  tabs: { display: "flex", gap: 4, background: "var(--bg-panel)", padding: 4, borderRadius: "var(--radius-md)", border: "1px solid var(--border-subtle)" },
  tab: {
    padding: "6px 14px", fontSize: 13, fontWeight: 500,
    background: "transparent", color: "var(--text-secondary)",
    border: "none", borderRadius: "var(--radius-sm)",
  },
  tabActive: { background: "var(--bg-hover)", color: "var(--text-primary)" },
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
  pager: { display: "flex", gap: 12, alignItems: "center", justifyContent: "center" },
  pageBtn: {
    height: 32, padding: "0 14px", fontSize: 13,
    background: "var(--bg-panel)", color: "var(--text-primary)",
    border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)",
  },
};
