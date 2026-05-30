import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { getWatchlist, removeWatchlist } from "../api/watchlist";
import { useToast } from "../components/Toast";
import { EmptyState } from "../components/EmptyState";
import { formatKrw } from "../lib/format";

function formatPrice(currency: string, price: number | null): string {
  if (price == null) return "-";
  return currency === "USD" ? `$${Number(price).toFixed(2)}` : formatKrw(price);
}

export default function WatchlistPage() {
  const qc = useQueryClient();
  const notify = useToast();
  const { data, isLoading } = useQuery({
    queryKey: ["watchlist"],
    queryFn: getWatchlist,
  });

  const remove = useMutation({
    mutationFn: (ticker: string) => removeWatchlist(ticker),
    onSuccess: () => {
      notify.success("관심종목에서 제거했습니다.");
      qc.invalidateQueries({ queryKey: ["watchlist"] });
    },
    onError: () => notify.error("제거에 실패했습니다."),
  });

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <section style={styles.head}>
        <h2 style={styles.title}>관심종목</h2>
        <span style={styles.count}>{data?.length ?? 0}종목</span>
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
              <th style={styles.thNum}></th>
            </tr>
          </thead>
          <tbody>
            {data?.map((s) => (
              <tr key={s.ticker} style={styles.tdRow}>
                <td style={styles.td}>
                  <Link to={`/stocks/${s.ticker}`} style={styles.link}>
                    <div style={{ fontWeight: 600 }}>{s.companyName}</div>
                    <div style={{ fontSize: 11, color: "var(--text-tertiary)" }}>{s.ticker}</div>
                  </Link>
                </td>
                <td style={styles.td}>{s.market}</td>
                <td style={styles.td}>{s.sector ?? "-"}</td>
                <td style={styles.tdNum}>{formatPrice(s.currency, s.currentPrice)}</td>
                <td style={styles.tdNum}>
                  <button
                    onClick={() => remove.mutate(s.ticker)}
                    disabled={remove.isPending}
                    style={styles.removeBtn}
                    aria-label="관심종목 제거"
                    title="관심종목에서 제거"
                  >★</button>
                </td>
              </tr>
            ))}
            {(!data || data.length === 0) && (
              <tr>
                <td colSpan={5}>
                  {isLoading ? (
                    <div style={styles.empty}>불러오는 중...</div>
                  ) : (
                    <EmptyState
                      icon="⭐"
                      title="관심종목이 없습니다"
                      desc="종목 상세 화면에서 ☆ 를 눌러 관심종목에 추가하세요."
                      to="/search"
                      ctaLabel="종목 검색하기"
                    />
                  )}
                </td>
              </tr>
            )}
          </tbody>
        </table>
        </div>
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
  link: { color: "var(--text-primary)" },
  removeBtn: {
    background: "transparent", border: "none", cursor: "pointer",
    color: "var(--color-warning)", fontSize: 18, lineHeight: 1, padding: "0 4px",
  },
};
