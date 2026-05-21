import { useQuery } from "@tanstack/react-query";
import ReactApexChart from "react-apexcharts";
import { getRisk } from "../api/risk";
import { useThemeStore } from "../store/themeStore";

export default function RiskPage() {
  const mode = useThemeStore((s) => s.mode);
  const { data, isLoading, error } = useQuery({
    queryKey: ["risk"],
    queryFn: getRisk,
    refetchInterval: 60_000,
  });

  if (isLoading) return <div style={{ color: "var(--text-tertiary)" }}>리스크 지표 계산 중...</div>;
  if (error || !data) return <div style={errStyle}>리스크 지표를 불러오지 못했습니다.</div>;

  return (
    <div style={{ display: "grid", gap: 20 }}>
      <section style={kpiRow}>
        <Kpi label="VaR 95% (1일)" value={pct(data.var95)} tone="down" />
        <Kpi label="VaR 99% (1일)" value={pct(data.var99)} tone="down" />
        <Kpi label="Sharpe Ratio" value={num(data.sharpe, 2)} tone={data.sharpe > 1 ? "up" : "flat"} />
        <Kpi label="Beta (vs KOSPI)" value={num(data.beta, 2)} tone="flat" />
      </section>
      <section style={kpiRow}>
        <Kpi label="Max Drawdown" value={pct(data.mdd)} tone="down" />
        <Kpi label="단일 종목 최대 비중" value={pct(data.concentration)} tone={data.concentration > 0.4 ? "down" : "flat"} />
      </section>

      {data.warnings.length > 0 && (
        <section style={warnBox}>
          <div style={{ fontWeight: 700, marginBottom: 8 }}>⚠️ 권장 임계치 초과</div>
          <ul style={{ margin: 0, paddingLeft: 18 }}>
            {data.warnings.map((w, i) => <li key={i}>{w}</li>)}
          </ul>
        </section>
      )}

      <section style={row2}>
        <Card title="지역 비중">
          <ShareDonut data={data.regionShare} mode={mode} />
        </Card>
        <Card title="섹터 비중">
          <ShareDonut data={data.sectorShare} mode={mode} />
        </Card>
      </section>
    </div>
  );
}

function ShareDonut({ data, mode }: { data: Record<string, number>; mode: "light" | "dark" }) {
  const labels = Object.keys(data);
  const series = labels.map((k) => Number((data[k] * 100).toFixed(2)));
  if (labels.length === 0) {
    return <div style={{ padding: 24, color: "var(--text-tertiary)", textAlign: "center" }}>데이터 없음</div>;
  }
  return (
    <ReactApexChart
      type="donut"
      height={260}
      series={series}
      options={{
        labels,
        legend: { position: "bottom", labels: { colors: "var(--text-secondary)" } },
        dataLabels: { enabled: true, formatter: (v: number) => `${v.toFixed(1)}%` },
        theme: { mode },
        tooltip: { y: { formatter: (v: number) => `${v.toFixed(2)}%` } },
        stroke: { width: 0 },
      }}
    />
  );
}

function Kpi({ label, value, tone }: { label: string; value: string; tone: "up" | "down" | "flat" }) {
  return (
    <div style={kpi}>
      <div style={kpiLabel}>{label}</div>
      <div style={kpiValue} className={tone === "flat" ? undefined : tone}>{value}</div>
    </div>
  );
}

function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section style={card}>
      <div style={cardTitle}>{title}</div>
      {children}
    </section>
  );
}

const pct = (n: number) => `${(n * 100).toFixed(2)}%`;
const num = (n: number, d: number) => Number.isFinite(n) ? n.toFixed(d) : "-";

const kpiRow: React.CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 12 };
const kpi: React.CSSProperties = {
  background: "var(--bg-panel)", border: "1px solid var(--border-subtle)",
  borderRadius: "var(--radius-lg)", padding: "16px 20px",
};
const kpiLabel: React.CSSProperties = { fontSize: 12, color: "var(--text-secondary)", marginBottom: 6 };
const kpiValue: React.CSSProperties = { fontSize: 22, fontWeight: 700, fontVariantNumeric: "tabular-nums" };
const row2: React.CSSProperties = { display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 };
const card: React.CSSProperties = {
  background: "var(--bg-panel)", border: "1px solid var(--border-subtle)",
  borderRadius: "var(--radius-lg)", padding: 20,
};
const cardTitle: React.CSSProperties = { fontSize: 13, fontWeight: 700, color: "var(--text-secondary)", marginBottom: 12 };
const warnBox: React.CSSProperties = {
  background: "var(--color-up-bg)", color: "var(--color-up)",
  border: "1px solid var(--color-up)", borderRadius: "var(--radius-lg)",
  padding: 16, fontSize: 13,
};
const errStyle: React.CSSProperties = {
  padding: 16, border: "1px solid var(--border-subtle)",
  borderRadius: "var(--radius-md)", background: "var(--color-up-bg)",
  color: "var(--color-danger)",
};
