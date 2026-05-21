import { useEffect, useRef, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { createChart, ColorType, LineSeries, createSeriesMarkers, type IChartApi, type ISeriesApi } from "lightweight-charts";
import { runBacktest, type BacktestRequest, type BacktestResponse } from "../api/backtest";
import { useThemeStore } from "../store/themeStore";
import { formatKrw, formatPct, trendClass } from "../lib/format";

const STRATEGIES: Array<{ value: BacktestRequest["strategy"]; label: string }> = [
  { value: "BUY_AND_HOLD",      label: "Buy & Hold" },
  { value: "MOVING_AVERAGE_20", label: "이동평균(MA20)" },
  { value: "RSI_14",            label: "RSI(14)" },
];

interface ApiError { code?: string; message?: string }

export default function BacktestPage() {
  const today = new Date();
  const lastYear = new Date(today.getTime() - 365 * 24 * 3600_000);
  const ymd = (d: Date) => d.toISOString().slice(0, 10);

  const [form, setForm] = useState<BacktestRequest>({
    strategy: "BUY_AND_HOLD",
    ticker: "005930",
    startDate: ymd(lastYear),
    endDate: ymd(today),
    initialCapital: 10_000_000,
  });

  const mutation = useMutation({
    mutationFn: runBacktest,
  });

  const ax = mutation.error as AxiosError<ApiError> | undefined;
  const errMsg = ax?.response?.data?.message ?? (mutation.error as Error | undefined)?.message;

  return (
    <div style={{ display: "grid", gap: 20 }}>
      <form
        style={card}
        onSubmit={(e) => { e.preventDefault(); mutation.mutate(form); }}
      >
        <div style={cardTitle}>전략 백테스트</div>
        <div style={grid}>
          <Field label="전략">
            <select value={form.strategy} onChange={(e) => setForm({ ...form, strategy: e.target.value as BacktestRequest["strategy"] })} style={input}>
              {STRATEGIES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
            </select>
          </Field>
          <Field label="종목 티커">
            <input value={form.ticker} onChange={(e) => setForm({ ...form, ticker: e.target.value })} style={input} required />
          </Field>
          <Field label="시작일">
            <input type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} style={input} required />
          </Field>
          <Field label="종료일">
            <input type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} style={input} required />
          </Field>
          <Field label="초기 자본(KRW)">
            <input type="number" min={1000} step={1000} value={form.initialCapital} onChange={(e) => setForm({ ...form, initialCapital: Number(e.target.value) })} style={input} required />
          </Field>
          <div style={{ display: "flex", alignItems: "flex-end" }}>
            <button type="submit" disabled={mutation.isPending} style={submitBtn}>
              {mutation.isPending ? "실행 중..." : "백테스트 실행"}
            </button>
          </div>
        </div>
        {errMsg && <div style={errStyle}>{errMsg}</div>}
      </form>

      {mutation.data && <ResultView result={mutation.data} />}
    </div>
  );
}

function ResultView({ result }: { result: BacktestResponse }) {
  const mode = useThemeStore((s) => s.mode);
  return (
    <>
      <section style={kpiRow}>
        <Kpi label="누적 수익률" value={formatPct(result.totalReturn * 100)} tone={trendClass(result.totalReturn)} />
        <Kpi label="최종 평가" value={formatKrw(result.finalValue)} />
        <Kpi label="MDD" value={formatPct(result.mdd * 100)} tone="down" />
        <Kpi label="Sharpe" value={result.sharpe.toFixed(2)} tone={result.sharpe > 1 ? "up" : "flat"} />
        <Kpi label="거래 횟수" value={String(result.tradeCount)} />
        <Kpi label="승률" value={formatPct(result.winRate * 100)} tone={result.winRate > 0.5 ? "up" : "down"} />
      </section>
      <section style={card}>
        <div style={cardTitle}>자산 곡선 + 매매 신호</div>
        <EquityChart result={result} mode={mode} />
      </section>
    </>
  );
}

function EquityChart({ result, mode }: { result: BacktestResponse; mode: "light" | "dark" }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;
    const chart = createChart(containerRef.current, {
      height: 360,
      autoSize: true,
      layout: {
        background: { type: ColorType.Solid, color: "transparent" },
        textColor: mode === "dark" ? "#A8B3C1" : "#4A525C",
      },
      grid: { vertLines: { color: mode === "dark" ? "#2A323D" : "#E6E8EB" }, horzLines: { color: mode === "dark" ? "#2A323D" : "#E6E8EB" } },
      timeScale: { borderVisible: false },
      rightPriceScale: { borderVisible: false },
    });
    const eq: ISeriesApi<"Line"> = chart.addSeries(LineSeries, { color: "#4F46E5", lineWidth: 2 });
    eq.setData(result.equityCurve.map((p) => ({ time: p.date as never, value: p.equity })));

    // 매매 마커 (lightweight-charts v5 플러그인)
    createSeriesMarkers(eq, result.trades.map((t) => ({
      time: t.date as never,
      position: t.side === "BUY" ? "belowBar" : "aboveBar",
      shape: t.side === "BUY" ? "arrowUp" : "arrowDown",
      color: t.side === "BUY" ? "#E74C3C" : "#3498DB",
      text: t.side,
    })));

    chartRef.current = chart;
    return () => { chart.remove(); chartRef.current = null; };
  }, [result, mode]);

  return <div ref={containerRef} style={{ width: "100%", height: 360 }} />;
}

function Kpi({ label, value, tone }: { label: string; value: string; tone?: "up" | "down" | "flat" }) {
  return (
    <div style={kpi}>
      <div style={kpiLabel}>{label}</div>
      <div style={kpiValue} className={tone && tone !== "flat" ? tone : undefined}>{value}</div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 12, color: "var(--text-secondary)" }}>
    {label}{children}
  </label>;
}

const card: React.CSSProperties = { background: "var(--bg-panel)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-lg)", padding: 20 };
const cardTitle: React.CSSProperties = { fontSize: 13, fontWeight: 700, color: "var(--text-secondary)", marginBottom: 16 };
const grid: React.CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))", gap: 12 };
const input: React.CSSProperties = { height: 40, padding: "0 12px", background: "var(--bg-base)", color: "var(--text-primary)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)", fontSize: 14 };
const submitBtn: React.CSSProperties = { height: 40, padding: "0 18px", background: "var(--color-primary)", color: "#fff", border: "none", borderRadius: "var(--radius-sm)", fontSize: 14, fontWeight: 600 };
const kpiRow: React.CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))", gap: 12 };
const kpi: React.CSSProperties = { background: "var(--bg-panel)", border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-lg)", padding: "14px 18px" };
const kpiLabel: React.CSSProperties = { fontSize: 12, color: "var(--text-secondary)", marginBottom: 6 };
const kpiValue: React.CSSProperties = { fontSize: 20, fontWeight: 700, fontVariantNumeric: "tabular-nums" };
const errStyle: React.CSSProperties = { marginTop: 12, padding: "8px 12px", fontSize: 13, color: "var(--color-danger)", background: "var(--color-up-bg)", borderRadius: "var(--radius-sm)" };
