import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { searchStocks } from "../api/stocks";
import { EmptyState } from "../components/EmptyState";
import { formatKrw } from "../lib/format";

const MARKETS = [
  { label: "전체", value: "" },
  { label: "KRX", value: "KRX" },
  { label: "NASDAQ", value: "NASDAQ" },
  { label: "NYSE", value: "NYSE" },
];

export default function StockSearchPage() {
  const [q, setQ] = useState("");
  const [market, setMarket] = useState("");
  const [page, setPage] = useState(0);

  const { data, isFetching } = useQuery({
    queryKey: ["stocks", q, market, page],
    queryFn: () => searchStocks({ q, market, page, size: 20 }),
    placeholderData: (prev) => prev,
  });

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <section style={styles.toolbar}>
        <input
          placeholder="종목명 또는 티커 검색"
          value={q}
          onChange={(e) => { setQ(e.target.value); setPage(0); }}
          style={styles.input}
        />
        <div style={styles.tabs}>
          {MARKETS.map((m) => (
            <button
              key={m.value}
              onClick={() => { setMarket(m.value); setPage(0); }}
              style={{
                ...styles.tab,
                ...(market === m.value ? styles.tabActive : {}),
              }}
            >{m.label}</button>
          ))}
        </div>
      </section>

      <section style={styles.card}>
        <div className="table-scroll">
        <table style={styles.table} className="tabular">
          <thead>
            <tr style={styles.thRow}>
              <th style={styles.th}>종목</th>
              <th style={styles.th}>시장</th>
              <th style={styles.th}>섹터</th>
              <th style={styles.thNum}>현재가</th>
            </tr>
          </thead>
          <tbody>
            {data?.items.map((s) => (
              <tr key={s.ticker} style={styles.tdRow}>
                <td style={styles.td}>
                  <Link to={`/stocks/${s.ticker}`} style={styles.link}>
                    <div style={{ fontWeight: 600 }}>{s.companyName}</div>
                    <div style={{ fontSize: 11, color: "var(--text-tertiary)" }}>{s.ticker}</div>
                  </Link>
                </td>
                <td style={styles.td}>{s.market}</td>
                <td style={styles.td}>{s.sector ?? "-"}</td>
                <td style={styles.tdNum}>
                  {s.currentPrice == null
                    ? "-"
                    : s.currency === "USD"
                      ? `$${Number(s.currentPrice).toFixed(2)}`
                      : formatKrw(s.currentPrice)}
                </td>
              </tr>
            ))}
            {(!data || data.items.length === 0) && (
              <tr><td colSpan={4}>
                {isFetching ? (
                  <div style={styles.empty}>검색 중...</div>
                ) : (
                  <EmptyState
                    icon="🔍"
                    title="검색 결과가 없습니다"
                    desc="종목명 또는 티커로 다시 검색해 보세요. (예: 삼성전자, AAPL)"
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
  toolbar: { display: "flex", gap: 12, alignItems: "center", flexWrap: "wrap" },
  input: {
    flex: "1 1 280px",
    height: 40, padding: "0 14px",
    background: "var(--bg-panel)", color: "var(--text-primary)",
    border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)",
    outline: "none", fontSize: 14,
  },
  tabs: { display: "flex", gap: 4, background: "var(--bg-panel)", padding: 4, borderRadius: "var(--radius-md)", border: "1px solid var(--border-subtle)" },
  tab: {
    padding: "6px 12px", fontSize: 13, fontWeight: 500,
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
  link: { color: "var(--text-primary)" },
  pager: { display: "flex", gap: 12, alignItems: "center", justifyContent: "center" },
  pageBtn: {
    height: 32, padding: "0 14px", fontSize: 13,
    background: "var(--bg-panel)", color: "var(--text-primary)",
    border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)",
  },
};
