import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import ReactApexChart from "react-apexcharts";
import { Link } from "react-router-dom";
import { getPortfolio } from "../api/portfolio";
import { getAiReports, analyzeNow, AI_TYPE_LABEL } from "../api/ai";
import { useToast } from "../components/Toast";
import { EmptyState } from "../components/EmptyState";
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
      <section className="grid-2">
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

        <AiCard />
      </section>

      {/* 보유 종목 */}
      <Card title={`보유 종목 (${data.holdings.length})`}>
        {data.holdings.length === 0 ? (
          <EmptyState
            icon="📈"
            title="아직 보유 종목이 없습니다"
            desc="종목을 검색해 첫 매수를 시도해 보세요. 가입 시 받은 시드머니로 바로 거래할 수 있어요."
            to="/search"
            ctaLabel="종목 검색하기"
          />
        ) : (
          <div className="table-scroll">
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
          </div>
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

function AiCard() {
  const qc = useQueryClient();
  const notify = useToast();
  const { data: reports, isLoading } = useQuery({
    queryKey: ["ai", "latest"],
    queryFn: () => getAiReports(1),
  });
  const latest = reports?.[0];

  const analyze = useMutation({
    mutationFn: analyzeNow,
    onSuccess: () => {
      notify.success("AI 분석을 생성했습니다.");
      qc.invalidateQueries({ queryKey: ["ai", "latest"] });
    },
    onError: (e) => {
      const ax = e as AxiosError<{ message?: string }>;
      notify.error(ax.response?.data?.message ?? "분석 생성에 실패했습니다.");
    },
  });

  return (
    <section style={styles.card}>
      <div style={styles.aiHead}>
        <div style={styles.cardTitle}>AI 코치</div>
        <button
          className="btn"
          onClick={() => analyze.mutate()}
          disabled={analyze.isPending}
          style={{ height: 30, fontSize: 12, padding: "0 12px" }}
        >
          {analyze.isPending ? "분석 중…" : "지금 분석"}
        </button>
      </div>

      {isLoading ? (
        <div className="skeleton" style={{ height: 120, borderRadius: "var(--radius-md)" }} />
      ) : latest ? (
        <div>
          <div style={styles.aiMeta}>
            <span style={styles.aiBadge}>{AI_TYPE_LABEL[latest.reportType]}</span>
            <span style={{ fontSize: 11, color: "var(--text-tertiary)" }}>
              {new Date(latest.createdAt).toLocaleString("ko-KR")}
            </span>
          </div>
          <div style={styles.aiContent}>{latest.content}</div>
        </div>
      ) : (
        <EmptyState
          icon="🤖"
          title="아직 AI 리포트가 없어요"
          desc="매매하면 자동 코멘트가 쌓이고 매주 일요일 회고가 생성됩니다. 지금 바로 분석을 받아볼 수도 있어요."
          onAction={() => analyze.mutate()}
          ctaLabel="지금 분석 받기"
        />
      )}
    </section>
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
  return (
    <div style={{ display: "grid", gap: 20 }}>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 12 }}>
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="skeleton" style={{ height: 84, borderRadius: "var(--radius-lg)" }} />
        ))}
      </div>
      <div className="grid-2">
        <div className="skeleton" style={{ height: 300, borderRadius: "var(--radius-lg)" }} />
        <div className="skeleton" style={{ height: 300, borderRadius: "var(--radius-lg)" }} />
      </div>
      <div className="skeleton" style={{ height: 200, borderRadius: "var(--radius-lg)" }} />
    </div>
  );
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
  aiHead: { display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 12 },
  aiMeta: { display: "flex", alignItems: "center", gap: 8, marginBottom: 8 },
  aiBadge: {
    fontSize: 11, fontWeight: 700, color: "var(--color-primary)",
    background: "rgba(79, 70, 229, 0.10)", padding: "2px 8px", borderRadius: 999,
  },
  aiContent: {
    fontSize: 13, lineHeight: 1.6, color: "var(--text-secondary)",
    whiteSpace: "pre-wrap", maxHeight: 200, overflowY: "auto",
  },
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
