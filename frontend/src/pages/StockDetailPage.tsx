import { useEffect, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  createChart,
  ColorType,
  CandlestickSeries,
  type IChartApi,
  type ISeriesApi,
} from "lightweight-charts";
import {
  getStock,
  getPriceHistory,
  getLastClose,
  type DailyCandle,
} from "../api/stocks";
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

  // 90일 일봉 — 차트 + lastClose 폴백 양쪽에 사용
  const { data: candles } = useQuery({
    queryKey: ["price-history", ticker],
    queryFn: () => getPriceHistory(ticker!, 90),
    enabled: !!ticker,
    staleTime: 60_000,
  });

  // 가장 최근 종가 (장 외 매매 안내용)
  const { data: lastClose } = useQuery({
    queryKey: ["last-close", ticker],
    queryFn: () => getLastClose(ticker!),
    enabled: !!ticker,
    staleTime: 60_000,
    retry: false,
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

  // 가격 우선순위: 실시간 → STOCKS.current_price → PRICE_HISTORY 최근 종가
  const livePrice = live?.price ?? null;
  const fallbackClose = lastClose?.close ?? null;
  const displayPrice =
    livePrice ?? stock.currentPrice ?? fallbackClose ?? 0;
  const isUsingClose = livePrice == null && stock.currentPrice == null && fallbackClose != null;
  const changePct = live?.changePct ?? 0;
  const cls = trendClass(changePct);

  // KRW 환산 (USD 종목 임시 환율)
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
          {livePrice != null && (
            <span style={{ marginLeft: 12, fontSize: 14 }} className={cls}>
              {formatPct(changePct)}
            </span>
          )}
          {isUsingClose && lastClose && (
            <div style={styles.closeBadge}>
              종가 ({lastClose.tradeDate})
            </div>
          )}
        </div>
      </header>

      <div style={styles.body}>
        <section style={styles.card}>
          <div style={styles.cardTitle}>
            일봉 (최근 {candles?.length ?? 0}일)
            {isUsingClose && <span style={styles.cardNote}> · 장 외 시간 — 종가 기준 매매 가능</span>}
          </div>
          <DailyCandleChart candles={candles ?? []} />
        </section>

        <TradePanel
          ticker={ticker}
          currentPriceKrw={estimatedKrw}
          priceSourceHint={isUsingClose ? `종가 ${lastClose?.tradeDate ?? ""}` : null}
        />
      </div>
    </div>
  );
}

function DailyCandleChart({ candles }: { candles: DailyCandle[] }) {
  const mode = useThemeStore((s) => s.mode);
  const containerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<"Candlestick"> | null>(null);

  // 차트 1회 생성 (테마 변경 시 재생성)
  useEffect(() => {
    if (!containerRef.current) return;
    const chart = createChart(containerRef.current, {
      height: 320,
      autoSize: true,
      layout: {
        background: { type: ColorType.Solid, color: "transparent" },
        textColor: mode === "dark" ? "#A8B3C1" : "#4A525C",
      },
      grid: {
        vertLines: { color: mode === "dark" ? "#2A323D" : "#E6E8EB" },
        horzLines: { color: mode === "dark" ? "#2A323D" : "#E6E8EB" },
      },
      timeScale: { borderVisible: false, timeVisible: false, secondsVisible: false },
      rightPriceScale: { borderVisible: false },
    });
    // 한국식: 상승 빨강, 하락 파랑
    const series = chart.addSeries(CandlestickSeries, {
      upColor: "#e74c3c", downColor: "#3498db",
      borderUpColor: "#e74c3c", borderDownColor: "#3498db",
      wickUpColor: "#e74c3c", wickDownColor: "#3498db",
    });
    chartRef.current = chart;
    seriesRef.current = series;
    return () => { chart.remove(); chartRef.current = null; seriesRef.current = null; };
  }, [mode]);

  // 데이터 갱신
  useEffect(() => {
    if (!seriesRef.current || candles.length === 0) return;
    const data = candles.map(c => ({
      time: c.time as never,        // 'YYYY-MM-DD'
      open: c.open,
      high: c.high,
      low: c.low,
      close: c.close,
    }));
    seriesRef.current.setData(data);
    chartRef.current?.timeScale().fitContent();
  }, [candles]);

  return <div ref={containerRef} style={{ width: "100%", height: 320 }} />;
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
  priceBox: { padding: "6px 12px", borderRadius: "var(--radius-sm)", textAlign: "right" },
  priceValue: { fontSize: 28, fontWeight: 700 },
  closeBadge: {
    fontSize: 11,
    color: "var(--color-warning)",
    marginTop: 4,
    fontWeight: 600,
  },
  body: { display: "grid", gridTemplateColumns: "1fr 340px", gap: 16 },
  card: {
    background: "var(--bg-panel)", border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-lg)", padding: 20,
  },
  cardTitle: {
    fontSize: 13, fontWeight: 700, color: "var(--text-secondary)", marginBottom: 12,
  },
  cardNote: {
    fontSize: 11, fontWeight: 500, color: "var(--color-warning)",
  },
};
