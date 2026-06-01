import { useQuery } from "@tanstack/react-query";
import ReactApexChart from "react-apexcharts";
import { getRanking, getMyTrend } from "../api/ranking";
import { EmptyState } from "../components/EmptyState";
import { formatKrw, formatPct, trendClass } from "../lib/format";
import { useThemeStore } from "../store/themeStore";

const MEDAL = ["🥇", "🥈", "🥉"];

export default function RankingPage() {
  const mode = useThemeStore((s) => s.mode);
  const { data: ranking, isLoading } = useQuery({ queryKey: ["ranking"], queryFn: () => getRanking(20) });
  const { data: trend } = useQuery({ queryKey: ["ranking", "trend"], queryFn: getMyTrend });

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <section style={styles.head}>
        <h2 style={styles.title}>수익률 랭킹</h2>
        {ranking?.asOf && <span style={styles.count}>{ranking.asOf} 기준 · {ranking.totalParticipants}명</span>}
      </section>

      {/* 내 순위 요약 */}
      <section style={styles.summary}>
        <div style={styles.summaryItem}>
          <div style={styles.sumLabel}>내 순위</div>
          <div style={styles.sumValue}>
            {ranking?.myRank != null ? `#${ranking.myRank}` : "-"}
            {ranking?.myRank != null && (
              <span style={styles.sumSub}> / {ranking.totalParticipants}명</span>
            )}
          </div>
        </div>
        <div style={styles.summaryItem}>
          <div style={styles.sumLabel}>내 수익률</div>
          <div style={{ ...styles.sumValue }} className={ranking?.myReturnPct != null ? trendClass(ranking.myReturnPct) : undefined}>
            {ranking?.myReturnPct != null ? formatPct(ranking.myReturnPct) : "-"}
          </div>
        </div>
      </section>

      {/* 내 자산 추이 */}
      <section style={styles.card}>
        <div style={styles.cardTitle}>내 자산 추이</div>
        {trend && trend.length > 0 ? (
          <ReactApexChart
            type="area"
            height={240}
            series={[{ name: "총 자산", data: trend.map((p) => [new Date(p.date).getTime(), Number(p.totalAssetKrw)]) }]}
            options={{
              chart: { toolbar: { show: false }, zoom: { enabled: false } },
              theme: { mode: mode === "dark" ? "dark" : "light" },
              colors: ["#4f46e5"],
              dataLabels: { enabled: false },
              stroke: { curve: "smooth", width: 2 },
              fill: { type: "gradient", gradient: { opacityFrom: 0.4, opacityTo: 0 } },
              xaxis: { type: "datetime", labels: { datetimeUTC: false } },
              yaxis: { labels: { formatter: (v: number) => `${Math.round(v / 10000)}만` } },
              tooltip: { x: { format: "yyyy-MM-dd" }, y: { formatter: (v: number) => formatKrw(v) } },
              grid: { borderColor: mode === "dark" ? "#2A323D" : "#E6E8EB" },
            }}
          />
        ) : (
          <EmptyState
            icon="📈"
            title="자산 추이 데이터가 쌓이는 중"
            desc="매일 자산 스냅샷이 기록됩니다. 며칠 지나면 추이 그래프가 그려져요."
          />
        )}
      </section>

      {/* 리더보드 */}
      <section style={styles.card}>
        <div style={styles.cardTitle}>리더보드 TOP {ranking?.entries.length ?? 0}</div>
        <div className="table-scroll">
          <table style={styles.table} className="tabular">
            <thead>
              <tr style={styles.thRow}>
                <th style={styles.th}>순위</th>
                <th style={styles.th}>사용자</th>
                <th style={styles.thNum}>수익률</th>
                <th style={styles.thNum}>총 자산</th>
              </tr>
            </thead>
            <tbody>
              {ranking?.entries.map((e) => (
                <tr key={e.rank} style={styles.tdRow}>
                  <td style={styles.td}>{e.rank <= 3 ? MEDAL[e.rank - 1] : e.rank}</td>
                  <td style={styles.td}>{e.username}</td>
                  <td style={styles.tdNum} className={trendClass(e.returnPct)}>{formatPct(e.returnPct)}</td>
                  <td style={styles.tdNum}>{formatKrw(e.totalAssetKrw)}</td>
                </tr>
              ))}
              {(!ranking || ranking.entries.length === 0) && (
                <tr><td colSpan={4}>
                  {isLoading ? (
                    <div style={styles.empty}>불러오는 중...</div>
                  ) : (
                    <EmptyState icon="🏆" title="아직 랭킹 데이터가 없습니다" desc="자산 스냅샷이 처음 기록되면 랭킹이 표시됩니다." />
                  )}
                </td></tr>
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
  summary: { display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 },
  summaryItem: {
    background: "var(--bg-panel)", border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-lg)", padding: "16px 20px",
  },
  sumLabel: { fontSize: 12, color: "var(--text-secondary)", marginBottom: 6 },
  sumValue: { fontSize: 24, fontWeight: 800, fontVariantNumeric: "tabular-nums" },
  sumSub: { fontSize: 13, fontWeight: 500, color: "var(--text-tertiary)" },
  card: {
    background: "var(--bg-panel)", border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-lg)", padding: 20,
  },
  cardTitle: { fontSize: 13, fontWeight: 700, color: "var(--text-secondary)", marginBottom: 12 },
  table: { width: "100%", borderCollapse: "collapse", fontSize: 13 },
  thRow: { borderBottom: "1px solid var(--border-subtle)" },
  th: { textAlign: "left" as const, padding: "10px 8px", color: "var(--text-secondary)", fontWeight: 600, fontSize: 12 },
  thNum: { textAlign: "right" as const, padding: "10px 8px", color: "var(--text-secondary)", fontWeight: 600, fontSize: 12 },
  tdRow: { borderBottom: "1px solid var(--border-subtle)" },
  td: { padding: "10px 8px" },
  tdNum: { padding: "10px 8px", textAlign: "right" as const },
  empty: { padding: 24, textAlign: "center" as const, color: "var(--text-tertiary)" },
};
