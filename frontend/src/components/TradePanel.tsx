import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { buyMarket, sellMarket } from "../api/trades";
import { registerLimitOrder } from "../api/limitOrders";
import { formatKrw } from "../lib/format";
import { useToast } from "./Toast";

interface ApiError { code?: string; message?: string }

type Side = "BUY" | "SELL";
type Method = "MARKET" | "LIMIT";

interface Props {
  ticker: string;
  currentPriceKrw: number | null;   // 예상 체결가(KRW 환산) — 시장가 추정용
  currency: string;                 // KRW | USD — 지정가 입력 단위
  currentPriceNative: number | null; // 종목 통화 기준 현재가 — 지정가 기본값
  /** "종가 2026-05-20" 같은 가격 출처 안내. null 이면 실시간. */
  priceSourceHint?: string | null;
}

export function TradePanel({ ticker, currentPriceKrw, currency, currentPriceNative, priceSourceHint }: Props) {
  const qc = useQueryClient();
  const notify = useToast();
  const [side, setSide] = useState<Side>("BUY");
  const [method, setMethod] = useState<Method>("MARKET");
  const [qty, setQty] = useState<string>("1");
  const [target, setTarget] = useState<string>(
    currentPriceNative ? String(currentPriceNative) : "",
  );
  const [error, setError] = useState<string | null>(null);

  const unit = currency === "USD" ? "$" : "₩";
  const sideLabel = side === "BUY" ? "매수" : "매도";

  const mutation = useMutation({
    mutationFn: async () => {
      const n = Number(qty);
      if (!Number.isFinite(n) || n <= 0) throw new Error("수량을 입력하세요.");
      if (method === "MARKET") {
        return side === "BUY" ? buyMarket(ticker, n) : sellMarket(ticker, n);
      }
      const t = Number(target);
      if (!Number.isFinite(t) || t <= 0) throw new Error("지정가를 입력하세요.");
      return registerLimitOrder({ ticker, orderType: side, targetPrice: t, quantity: n });
    },
    onSuccess: (r) => {
      setError(null);
      if (method === "MARKET") {
        const o = r as Awaited<ReturnType<typeof buyMarket>>;
        notify.success(`${sideLabel} 체결 · ${o.quantity}주 · ${formatKrw(o.totalAmountKrw)}`);
        qc.invalidateQueries({ queryKey: ["portfolio"] });
        qc.invalidateQueries({ queryKey: ["history"] });
      } else {
        notify.success(`지정가 ${sideLabel} 등록 · ${unit}${Number(target).toLocaleString()} × ${qty}주`);
        qc.invalidateQueries({ queryKey: ["limitOrders"] });
      }
    },
    onError: (e) => {
      const ax = e as AxiosError<ApiError>;
      const msg = ax.response?.data?.message ?? (e as Error).message ?? "주문 실패";
      setError(msg);
      notify.error(msg);
    },
  });

  const qtyNum = Number(qty);
  const marketEstimate = currentPriceKrw != null && qtyNum > 0 ? currentPriceKrw * qtyNum : 0;
  const limitEstimateNative = Number(target) > 0 && qtyNum > 0 ? Number(target) * qtyNum : 0;

  const disabled =
    mutation.isPending ||
    (method === "MARKET" ? currentPriceKrw == null : !(Number(target) > 0));

  return (
    <div style={styles.shell}>
      {/* 매수/매도 */}
      <div style={styles.tabs}>
        <button onClick={() => setSide("BUY")} style={tabStyle(side === "BUY", "up")}>매수</button>
        <button onClick={() => setSide("SELL")} style={tabStyle(side === "SELL", "down")}>매도</button>
      </div>

      {/* 시장가/지정가 */}
      <div style={styles.methodTabs}>
        <button
          onClick={() => setMethod("MARKET")}
          style={methodStyle(method === "MARKET")}
        >시장가</button>
        <button
          onClick={() => setMethod("LIMIT")}
          style={methodStyle(method === "LIMIT")}
        >지정가</button>
      </div>

      {method === "LIMIT" && (
        <div style={styles.row}>
          <label style={styles.label}>지정가</label>
          <div style={styles.inputWrap}>
            <span style={styles.unit}>{unit}</span>
            <input
              inputMode="decimal"
              value={target}
              onChange={(e) => setTarget(e.target.value.replace(/[^0-9.]/g, ""))}
              style={{ ...styles.input, paddingLeft: 26 }}
            />
          </div>
        </div>
      )}

      <div style={styles.row}>
        <label style={styles.label}>수량</label>
        <input
          inputMode="decimal"
          value={qty}
          onChange={(e) => setQty(e.target.value.replace(/[^0-9.]/g, ""))}
          style={styles.input}
        />
      </div>

      <div style={styles.sliderRow}>
        {[10, 25, 50, 100].map((n) => (
          <button key={n} onClick={() => setQty(String(n))} style={styles.pctBtn}>+{n}</button>
        ))}
      </div>

      <div style={styles.estimateBox}>
        <span style={styles.estimateLabel}>
          {method === "MARKET" ? "예상 체결금액" : "지정가 주문금액"}
        </span>
        <span style={styles.estimateValue} className="tabular">
          {method === "MARKET"
            ? formatKrw(marketEstimate)
            : `${unit}${limitEstimateNative.toLocaleString()}`}
        </span>
      </div>

      {method === "LIMIT" ? (
        <div style={styles.sourceHint}>
          ⏱ 현재가가 지정가에 {side === "BUY" ? "도달(이하)" : "도달(이상)"}하면 자동 체결됩니다 · 기본 30일 유효
        </div>
      ) : priceSourceHint ? (
        <div style={styles.sourceHint}>
          💡 {priceSourceHint} 기준으로 모의 체결됩니다 (장 외 시간)
        </div>
      ) : null}

      {error && <div style={styles.error}>{error}</div>}

      <button
        onClick={() => mutation.mutate()}
        disabled={disabled}
        style={{
          ...styles.submitBtn,
          background: side === "BUY" ? "var(--color-up)" : "var(--color-down)",
          opacity: disabled ? 0.6 : 1,
        }}
      >
        {mutation.isPending
          ? "처리 중..."
          : method === "MARKET"
            ? `${sideLabel} 주문`
            : `지정가 ${sideLabel} 등록`}
      </button>
    </div>
  );
}

function tabStyle(active: boolean, tone: "up" | "down"): React.CSSProperties {
  return {
    flex: 1, height: 40, fontSize: 14, fontWeight: 600,
    background: active ? (tone === "up" ? "var(--color-up-bg)" : "var(--color-down-bg)") : "transparent",
    color: active ? `var(--color-${tone})` : "var(--text-secondary)",
    border: "none", borderRadius: "var(--radius-sm)",
    cursor: "pointer",
  };
}

function methodStyle(active: boolean): React.CSSProperties {
  return {
    flex: 1, height: 32, fontSize: 12, fontWeight: 600,
    background: active ? "var(--bg-hover)" : "transparent",
    color: active ? "var(--text-primary)" : "var(--text-secondary)",
    border: "none", borderRadius: "var(--radius-sm)",
    cursor: "pointer",
  };
}

const styles: Record<string, React.CSSProperties> = {
  shell: {
    background: "var(--bg-panel)", border: "1px solid var(--border-subtle)",
    borderRadius: "var(--radius-lg)", padding: 20,
    display: "flex", flexDirection: "column", gap: 14,
  },
  tabs: {
    display: "flex", gap: 4, padding: 4,
    background: "var(--bg-elevated)", borderRadius: "var(--radius-md)",
  },
  methodTabs: {
    display: "flex", gap: 4, padding: 3,
    background: "var(--bg-elevated)", borderRadius: "var(--radius-sm)",
  },
  row: { display: "flex", alignItems: "center", gap: 12 },
  label: { fontSize: 13, color: "var(--text-secondary)", width: 48 },
  inputWrap: { position: "relative", flex: 1 },
  unit: {
    position: "absolute", left: 12, top: "50%", transform: "translateY(-50%)",
    fontSize: 14, color: "var(--text-tertiary)", pointerEvents: "none",
  },
  input: {
    flex: 1, width: "100%", height: 42, padding: "0 14px",
    fontSize: 18, fontWeight: 600, textAlign: "right" as const,
    background: "var(--bg-base)", color: "var(--text-primary)",
    border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)",
    fontVariantNumeric: "tabular-nums",
  },
  sliderRow: { display: "flex", gap: 6 },
  pctBtn: {
    flex: 1, height: 32, fontSize: 12, fontWeight: 600,
    background: "var(--bg-elevated)", color: "var(--text-secondary)",
    border: "1px solid var(--border-subtle)", borderRadius: "var(--radius-sm)",
  },
  estimateBox: {
    display: "flex", justifyContent: "space-between", alignItems: "center",
    padding: "10px 12px",
    background: "var(--bg-elevated)", borderRadius: "var(--radius-sm)",
  },
  estimateLabel: { fontSize: 12, color: "var(--text-secondary)" },
  estimateValue: { fontSize: 16, fontWeight: 700 },
  error: {
    fontSize: 12, color: "var(--color-danger)",
    background: "var(--color-up-bg)", padding: "8px 12px",
    borderRadius: "var(--radius-sm)",
  },
  sourceHint: {
    fontSize: 11, color: "var(--color-warning)",
    background: "rgba(245, 158, 11, 0.10)", padding: "8px 12px",
    borderRadius: "var(--radius-sm)",
    lineHeight: 1.4,
  },
  submitBtn: {
    height: 48, color: "#fff",
    border: "none", borderRadius: "var(--radius-sm)",
    fontSize: 15, fontWeight: 700,
  },
};
