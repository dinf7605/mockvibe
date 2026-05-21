import { useEffect, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { createChart, ColorType, LineSeries, type IChartApi, type ISeriesApi } from "lightweight-charts";
import { getStock } from "../api/stocks";
import { useStompPrice } from "../hooks/useStompPrice";
import { TradePanel } from "../components/TradePanel";
import { formatKrw, formatPct, trendClass } from "../lib/format";
import { useThemeStore } from "../store/themeStore";

export default function StockDetailPage() {
  const { ticker } = useParams<{ ticker: string }>();
  const { data: stock } = useQuery({
    queryKey: ["stock", ticker],
    queryFn: () => getStock(ticker!),
    enabled: !!ticker,
  });

  const live = useStompPrice(ticker);

  // 가격 깜빡임
  const prevPriceRef = useRef<number | null>(null);
  const [flashKey, setFlashKey] = useState(0);
  const [flashDir, setFlashDir] = useState<"up" | "down" | "">("");
  useEffect(() => {
    if (live?.price == null) return;
    const prev = prevPriceRef.current;
    if (prev != null && prev !== live.price) {
      setFlashDir(live.price > prev ? "up" : "down");
      setFlashKey((k) => k + 1);
    }
    prevPriceRef.current = live.price;
  }, [live?.price]);

  if (!ticker || !stock) return <div style={{ color: "var(--text-tertiary)" }}>불러오는 중...</div>;

  const displayPrice = live?.price ?? stock.currentPrice ?? 0;
  const changePct = live?.changePct ?? 0;
  const cls = trendClass(changePct);
  // KRW 환산 — USD 종목은 임시 1380 (실제 환율은 백엔드가 fxRate로 계산)
  const fxRate = stock.currency === "USD" ? 1380 : 1;
  const estimatedKrw = displayPrice ? displayPrice * fxRate : null;

  return (
    <div style={styles.grid}>
      <header style={styles.header}>
        <div>
          <div style={styles.companyName}>{stock.companyName}</div>
          <div style={styles.tickerLine}>{stock.ticker} · {stock.market} · {stock.sector ?? "-"}</div>
        </div>
        <div key={flashKey} className="price-flash tabular" data-flash={flashDir} style={styles.priceBox}>
          <span style={styles.priceValue} className={cls}>
            {stock.currency === "USD" ? `$${displayPrice.toFixed(2)}` : formatKrw(displayPrice)}
          </span>
          <span style={{ marginLeft: 12, fontSize: 14 }} className={cls}>
            {formatPct(changePct)}
          </span>
        </div>
      </header>

      <div style={styles.body}>
        <section style={styles.card}>
          <div style={styles.cardTitle}>실시간 가격 추이</div>
          <LiveLineChart ticker={ticker} live={live?.price ?? null} />
          <div style={styles.chartNote}>일별 OHLC 캔들은 D26 PRICE_HISTORY 적재 후 표시됩니다.</div>
        </section>

        <TradePanel ticker={ticker} currentPriceKrw={estimatedKrw} />
      </div>
    </div>
  );
}

function LiveLineChart({ ticker, live }: { ticker: string; live: number | null }) {
  const mode = useThemeStore((s) => s.mode);
  const containerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<"Line"> | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;
    const chart = createChart(containerRef.current, {
      height: 280,
      autoSize: true,
      layout: {
        background: { type: ColorType.Solid, color: "transparent" },
        textColor: mode === "dark" ? "#A8B3C1" : "#4A525C",
      },
      grid: {
        vertLines: { color: mode === "dark" ? "#2A323D" : "#E6E8EB" },
        horzLines: { color: mode === "dark" ? "#2A323D" : "#E6E8EB" },
      },
      timeScale: { borderVisible: false, timeVisible: true, secondsVisible: false },
      rightPriceScale: { borderVisible: false },
    });
    const series = chart.addSeries(LineSeries, { color: "#4F46E5", lineWidth: 2 });
    chartRef.current = chart;
    seriesRef.current = series;
    return () => { chart.remove(); chartRef.current = null; seriesRef.current = null; };
  }, [ticker, mode]);

  useEffect(() => {
    if (live == null || !seriesRef.current) return;
    seriesRef.current.update({
      time: Math.floor(Date.now() / 1000) as never,
      value: live,
    });
  }, [live]);

  return <div ref={containerRef} style={{ width: "100%", height: 280 }} />;
}

const styles: Record<string, React.CSSProperties> = {
  grid: { display: "grid", gap: 20 },
  header: {
    display: "flex", justifyContent: "space-between", alignItems: "center",
    padding: 20,
    background: "var(--bg-panel)", border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-lg)",
  },
  companyName: { fontSize: 20, fontWeight: 700 },
  tickerLine: { fontSize: 12, color: "var(--text-secondary)", marginTop: 4 },
  priceBox: { padding: "6px 12px", borderRadius: "var(--radius-sm)" },
  priceValue: { fontSize: 28, fontWeight: 700 },
  body: { display: "grid", gridTemplateColumns: "1fr 340px", gap: 16 },
  card: {
    background: "var(--bg-panel)", border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-lg)", padding: 20,
  },
  cardTitle: { fontSize: 13, fontWeight: 700, color: "var(--text-secondary)", marginBottom: 12 },
  chartNote: { fontSize: 12, color: "var(--text-tertiary)", marginTop: 8 },
};
