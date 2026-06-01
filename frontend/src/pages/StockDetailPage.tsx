import { useEffect, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
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
import { isWatching, addWatchlist, removeWatchlist } from "../api/watchlist";
import { createAlert, type AlertDirection } from "../api/alerts";
import { getStockNews } from "../api/news";
import { useToast } from "../components/Toast";
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

  // 관심종목 여부 + 토글
  const qc = useQueryClient();
  const notify = useToast();
  const { data: watching } = useQuery({
    queryKey: ["watching", ticker],
    queryFn: () => isWatching(ticker!),
    enabled: !!ticker,
  });
  const toggleWatch = useMutation({
    mutationFn: () =>
      watching ? removeWatchlist(ticker!) : addWatchlist(ticker!),
    onSuccess: () => {
      notify.success(watching ? "관심종목에서 제거했습니다." : "관심종목에 추가했습니다.");
      qc.invalidateQueries({ queryKey: ["watching", ticker] });
      qc.invalidateQueries({ queryKey: ["watchlist"] });
    },
    onError: () => notify.error("관심종목 변경에 실패했습니다."),
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
          <div style={styles.companyNameRow}>
            <span style={styles.companyName}>{stock.companyName}</span>
            <button
              onClick={() => toggleWatch.mutate()}
              disabled={toggleWatch.isPending}
              style={{ ...styles.starBtn, color: watching ? "var(--color-warning)" : "var(--text-tertiary)" }}
              aria-label={watching ? "관심종목 제거" : "관심종목 추가"}
              title={watching ? "관심종목에서 제거" : "관심종목에 추가"}
            >{watching ? "★" : "☆"}</button>
          </div>
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

      <div className="detail-body">
        <section style={styles.card}>
          <div style={styles.cardTitle}>
            일봉 (최근 {candles?.length ?? 0}일)
            {isUsingClose && <span style={styles.cardNote}> · 장 외 시간 — 종가 기준 매매 가능</span>}
          </div>
          <DailyCandleChart candles={candles ?? []} />
        </section>

        <div style={styles.sideCol}>
          <TradePanel
            ticker={ticker}
            currentPriceKrw={estimatedKrw}
            currency={stock.currency}
            currentPriceNative={displayPrice || null}
            priceSourceHint={isUsingClose ? `종가 ${lastClose?.tradeDate ?? ""}` : null}
          />
          <AlertCard ticker={ticker} currency={stock.currency} defaultPrice={displayPrice} />
        </div>
      </div>

      <NewsCard ticker={ticker} />
    </div>
  );
}

function NewsCard({ ticker }: { ticker: string }) {
  const { data } = useQuery({
    queryKey: ["news", ticker],
    queryFn: () => getStockNews(ticker, 3),
    staleTime: 300_000,
    retry: false,
  });
  if (!data || data.length === 0) return null;  // KRX·뉴스없음·키없음이면 카드 숨김

  return (
    <section style={styles.card}>
      <div style={styles.cardTitle}>종목 뉴스</div>
      <ul style={newsStyles.list}>
        {data.slice(0, 8).map((n, i) => (
          <li key={n.url + i} style={newsStyles.item}>
            <a href={n.url} target="_blank" rel="noreferrer" style={newsStyles.headline}>
              {n.headline}
            </a>
            <div style={newsStyles.meta}>
              {n.source} · {new Date(n.datetime * 1000).toLocaleDateString("ko-KR")}
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}

const newsStyles: Record<string, React.CSSProperties> = {
  list: { listStyle: "none", margin: 0, padding: 0, display: "flex", flexDirection: "column", gap: 12 },
  item: { borderBottom: "1px solid var(--border-subtle)", paddingBottom: 12 },
  headline: { fontSize: 14, fontWeight: 600, color: "var(--text-primary)", lineHeight: 1.4 },
  meta: { fontSize: 11, color: "var(--text-tertiary)", marginTop: 4 },
};

function AlertCard({
  ticker,
  currency,
  defaultPrice,
}: {
  ticker: string;
  currency: string;
  defaultPrice: number;
}) {
  const qc = useQueryClient();
  const notify = useToast();
  const [direction, setDirection] = useState<AlertDirection>("ABOVE");
  const [price, setPrice] = useState<string>(defaultPrice ? String(defaultPrice) : "");

  const create = useMutation({
    mutationFn: () =>
      createAlert({ ticker, direction, targetPrice: Number(price) }),
    onSuccess: () => {
      notify.success(`알림 추가 · ${direction === "ABOVE" ? "이상" : "이하"} ${Number(price).toLocaleString()}`);
      qc.invalidateQueries({ queryKey: ["alerts"] });
      qc.invalidateQueries({ queryKey: ["alerts", "triggered-count"] });
    },
    onError: () => notify.error("알림 추가에 실패했습니다."),
  });

  const valid = price !== "" && Number(price) > 0;
  const unit = currency === "USD" ? "$" : "₩";

  return (
    <section style={alertStyles.card}>
      <div style={alertStyles.title}>가격 알림</div>
      <div style={alertStyles.row}>
        <button
          onClick={() => setDirection("ABOVE")}
          style={{ ...alertStyles.dirBtn, ...(direction === "ABOVE" ? alertStyles.dirActive : {}) }}
        >이상 ▲</button>
        <button
          onClick={() => setDirection("BELOW")}
          style={{ ...alertStyles.dirBtn, ...(direction === "BELOW" ? alertStyles.dirActive : {}) }}
        >이하 ▼</button>
      </div>
      <label style={alertStyles.label}>목표가 ({unit})</label>
      <input
        type="number"
        value={price}
        min={0}
        step="any"
        onChange={(e) => setPrice(e.target.value)}
        style={alertStyles.input}
      />
      <button
        onClick={() => create.mutate()}
        disabled={!valid || create.isPending}
        style={{ ...alertStyles.submit, opacity: valid && !create.isPending ? 1 : 0.5 }}
      >
        {create.isSuccess ? "알림 추가됨 ✓" : "알림 추가"}
      </button>
      {create.isError && (
        <div style={alertStyles.err}>알림 추가에 실패했습니다.</div>
      )}
    </section>
  );
}

const alertStyles: Record<string, React.CSSProperties> = {
  card: {
    background: "var(--bg-panel)", border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-lg)", padding: 16, display: "grid", gap: 10,
  },
  title: { fontSize: 13, fontWeight: 700, color: "var(--text-secondary)" },
  row: { display: "flex", gap: 6 },
  dirBtn: {
    flex: 1, height: 32, fontSize: 13, cursor: "pointer",
    background: "transparent", color: "var(--text-secondary)",
    border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)",
  },
  dirActive: { background: "var(--bg-hover)", color: "var(--text-primary)", fontWeight: 600 },
  label: { fontSize: 11, color: "var(--text-tertiary)" },
  input: {
    height: 36, padding: "0 12px", fontSize: 14,
    background: "var(--bg-base)", color: "var(--text-primary)",
    border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)", outline: "none",
  },
  submit: {
    height: 36, fontSize: 13, fontWeight: 600, cursor: "pointer",
    background: "var(--color-primary)", color: "#fff",
    border: "none", borderRadius: "var(--radius-sm)",
  },
  err: { fontSize: 11, color: "var(--color-down)" },
};

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
  companyNameRow: { display: "flex", alignItems: "center", gap: 8 },
  companyName: { fontSize: 20, fontWeight: 700 },
  starBtn: {
    background: "transparent", border: "none", cursor: "pointer",
    fontSize: 22, lineHeight: 1, padding: 0,
  },
  tickerLine: { fontSize: 12, color: "var(--text-secondary)", marginTop: 4 },
  priceBox: { padding: "6px 12px", borderRadius: "var(--radius-sm)", textAlign: "right" },
  priceValue: { fontSize: 28, fontWeight: 700 },
  closeBadge: {
    fontSize: 11,
    color: "var(--color-warning)",
    marginTop: 4,
    fontWeight: 600,
  },
  sideCol: { display: "grid", gap: 16, alignContent: "start" },
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
