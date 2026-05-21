import { useQuery } from "@tanstack/react-query";
import ReactApexChart from "react-apexcharts";
import { Link } from "react-router-dom";
import { getPortfolio } from "../api/portfolio";
import { formatKrw, formatPct, trendClass } from "../lib/format";
import { useThemeStore } from "../store/themeStore";

export default function DashboardPage() {
  const mode = useThemeStore((s) => s.mode);
  const { data, isLoading, error } = useQuery({
    queryKey: ["portfolio"],
    queryFn: getPortfolio,
    refetchInterval: 5_000,    // 5초 폴링 (D19 STOMP 구독 후 제거)
  });

  if (isLoading) return <Skeleton />;
  if (error || !data) return <ErrorBox message="포트폴리오를 불러오지 못했습니다." />;

  const totalPnlClass = trendClass(data.totalPnlKrw);

  return (
    <div style={{ display: "grid", gap: 20 }}>
      {/* 상단 KPI 카드 */}
      <section style={styles.kpiRow}>
        <Kpi label="총 자산" value={formatKrw(data.totalAssetKrw)} highlight />
        <Kpi label="예수금" value={formatKrw(data.cashBalanceKrw)} />
        <Kpi label="평가 금액" value={formatKrw(data.holdingValueKrw)} />
        <Kpi
          label="평가 손익"
          value={`${formatKrw(data.totalPnlKrw)}  (${formatPct(data.totalPnlPct)})`}
          tone={totalPnlClass}
        />
      </section>

      {/* 자산 비중 도넛 + AI 위클리 자리 */}
      <section style={styles.row2}>
        <Card title="자산 비중">
          <ReactApexChart
            type="donut"
            height={260}
            series={[data.regionShare.kr, data.regionShare.us, data.regionShare.cash]}
            options={{
              labels: ["한국 주식", "미국 주식", "예수금"],
              legend: { position: "bottom", labels: { colors: "var(--text-secondary)" } },
              dataLabels: { enabled: true, formatter: (v: number) => `${v.toFixed(1)}%` },
              theme: { mode: mode === "dark" ? "dark" : "light" },
              colors: ["#E74C3C", "#3498DB", "#8B95A1"],
              tooltip: { y: { formatter: (v: number) => `${v.toFixed(2)}%` } },
              stroke: { width: 0 },
            }}
          />
        </Card>

        <Card title="AI 위클리 리포트 · 예정 D39">
          <div style={styles.placeholderBox}>
            Phase 6에서 Claude API 연동 후 매주 일요일 자동 생성됩니다.
          </div>
        </Card>
      </section>

      {/* 보유 종목 */}
      <Card title={`보유 종목 (${data.holdings.length})`}>
        {data.holdings.length === 0 ? (
          <div style={styles.placeholderBox}>
            아직 보유 종목이 없습니다. <Link to="/search" style={styles.link}>종목 검색</Link>에서 첫 매수를 시도해 보세요.
          </div>
        ) : (
          <table style={styles.table} className="tabular">
            <thead>
              <tr style={styles.thRow}>
                <th style={styles.th}>종목</th>
                <th style={styles.thNum}>수량</th>
                <th style={styles.thNum}>평균단가</th>
                <th style={styles.thNum}>현재가(KRW)</th>
                <th style={styles.thNum}>평가금액</th>
                <th style={styles.thNum}>손익</th>
              </tr>
            </thead>
            <tbody>
              {data.holdings.map((h) => (
                <tr key={h.ticker} style={styles.tdRow}>
                  <td style={styles.td}>
                    <Link to={`/stocks/${h.ticker}`} style={styles.link}>
                      <div style={{ fontWeight: 600 }}>{h.companyName}</div>
                      <div style={{ fontSize: 11, color: "var(--text-tertiary)" }}>
                        {h.ticker} · {h.market}
                      </div>
                    </Link>
                  </td>
                  <td style={styles.tdNum}>{h.quantity}</td>
                  <td style={styles.tdNum}>{formatKrw(h.averagePriceKrw)}</td>
                  <td style={styles.tdNum}>{formatKrw(h.currentPriceKrw)}</td>
                  <td style={styles.tdNum}>{formatKrw(h.evaluationKrw)}</td>
                  <td style={{ ...styles.tdNum }} className={trendClass(h.pnlKrw)}>
                    {formatKrw(h.pnlKrw)}<br />
                    <span style={{ fontSize: 11 }}>{formatPct(h.pnlPct)}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </div>
  );
}

// ===== 작은 컴포넌트들 =====

function Kpi({ label, value, highlight, tone }: { label: string; value: string; highlight?: boolean; tone?: "up" | "down" | "flat" }) {
  return (
    <div style={{ ...styles.kpi, ...(highlight ? styles.kpiHighlight : {}) }}>
      <div style={styles.kpiLabel}>{label}</div>
      <div style={styles.kpiValue} className={tone === "up" ? "up" : tone === "down" ? "down" : undefined}>{value}</div>
    </div>
  );
}

function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section style={styles.card}>
      <div style={styles.cardTitle}>{title}</div>
      <div>{children}</div>
    </section>
  );
}

function Skeleton() {
  return <div style={{ color: "var(--text-tertiary)" }}>불러오는 중...</div>;
}

function ErrorBox({ message }: { message: string }) {
  return (
    <div style={{
      padding: 16, border: "1px solid var(--border-subtle)",
      borderRadius: "var(--radius-md)", background: "var(--color-up-bg)",
      color: "var(--color-danger)",
    }}>{message}</div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  kpiRow: { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 12 },
  kpi: {
    background: "var(--bg-panel)", border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-lg)", padding: "16px 20px",
  },
  kpiHighlight: { boxShadow: "var(--shadow-md)" },
  kpiLabel: { fontSize: 12, color: "var(--text-secondary)", marginBottom: 6 },
  kpiValue: { fontSize: 22, fontWeight: 700, fontVariantNumeric: "tabular-nums" },
  row2: { display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 },
  card: {
    background: "var(--bg-panel)", border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-lg)", padding: 20,
  },
  cardTitle: { fontSize: 13, fontWeight: 700, color: "var(--text-secondary)", marginBottom: 12 },
  placeholderBox: {
    padding: 24, color: "var(--text-tertiary)", fontSize: 13, textAlign: "center" as const,
  },
  table: { width: "100%", borderCollapse: "collapse", fontSize: 13 },
  thRow: { borderBottom: "1px solid var(--border-subtle)" },
  th: { textAlign: "left" as const, padding: "10px 8px", color: "var(--text-secondary)", fontWeight: 600, fontSize: 12 },
  thNum: { textAlign: "right" as const, padding: "10px 8px", color: "var(--text-secondary)", fontWeight: 600, fontSize: 12 },
  tdRow: { borderBottom: "1px solid var(--border-subtle)" },
  td: { padding: "10px 8px" },
  tdNum: { padding: "10px 8px", textAlign: "right" as const },
  link: { color: "var(--text-primary)" },
};
