import { useQuery } from "@tanstack/react-query";
import ReactApexChart from "react-apexcharts";
import { getDailyAiUsage } from "../../api/admin";
import { useThemeStore } from "../../store/themeStore";

export function AdminAiCostPage() {
  const mode = useThemeStore((s) => s.mode);
  const { data, isLoading } = useQuery({ queryKey: ["admin-ai-usage"], queryFn: getDailyAiUsage, refetchInterval: 30_000 });

  if (isLoading) return <div style={{ color: "var(--text-tertiary)" }}>집계 중...</div>;
  if (!data || Object.keys(data).length === 0) {
    return <section style={card}><div style={cardTitle}>AI 토큰 사용량</div>
      <div style={{ padding: 24, color: "var(--text-tertiary)", textAlign: "center" as const }}>
        최근 30일 AI 호출 기록이 없습니다.
      </div></section>;
  }

  // 날짜 정렬
  const days = Object.keys(data).sort();
  const tokens = days.map((d) => data[d]);
  const total = tokens.reduce((a, b) => a + b, 0);

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <section style={kpiRow}>
        <Kpi label="누적 토큰 (30일)" value={total.toLocaleString("ko-KR")} />
        <Kpi label="호출 일수" value={String(days.length)} />
        <Kpi label="평균/일" value={Math.round(total / days.length).toLocaleString("ko-KR")} />
      </section>
      <section style={card}>
        <div style={cardTitle}>일별 토큰 사용량</div>
        <ReactApexChart
          type="bar"
          height={320}
          series={[{ name: "tokens", data: tokens }]}
          options={{
            chart: { toolbar: { show: false } },
            xaxis: { categories: days, labels: { style: { colors: "var(--text-secondary)" } } },
            yaxis: { labels: { style: { colors: "var(--text-secondary)" } } },
            theme: { mode },
            colors: ["#4F46E5"],
            grid: { borderColor: mode === "dark" ? "#2A323D" : "#E6E8EB" },
            tooltip: { y: { formatter: (v: number) => `${v.toLocaleString("ko-KR")} tokens` } },
            dataLabels: { enabled: false },
          }}
        />
      </section>
    </div>
  );
}

function Kpi({ label, value }: { label: string; value: string }) {
  return <div style={kpi}><div style={kpiLabel}>{label}</div><div style={kpiValue}>{value}</div></div>;
}

const kpiRow: React.CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 12 };
const kpi: React.CSSProperties = { background: "var(--bg-panel)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-lg)", padding: "16px 20px" };
const kpiLabel: React.CSSProperties = { fontSize: 12, color: "var(--text-secondary)", marginBottom: 6 };
const kpiValue: React.CSSProperties = { fontSize: 22, fontWeight: 700, fontVariantNumeric: "tabular-nums" };
const card: React.CSSProperties = { background: "var(--bg-panel)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-lg)", padding: 20 };
const cardTitle: React.CSSProperties = { fontSize: 13, fontWeight: 700, color: "var(--text-secondary)", marginBottom: 12 };
