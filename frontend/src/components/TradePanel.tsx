import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { buyMarket, sellMarket } from "../api/trades";
import { formatKrw } from "../lib/format";

interface ApiError { code?: string; message?: string }

interface Props {
  ticker: string;
  currentPriceKrw: number | null;   // 예상 체결가(KRW 환산)
  /** "종가 2026-05-20" 같은 가격 출처 안내. null 이면 실시간. */
  priceSourceHint?: string | null;
}

export function TradePanel({ ticker, currentPriceKrw, priceSourceHint }: Props) {
  const qc = useQueryClient();
  const [side, setSide] = useState<"BUY" | "SELL">("BUY");
  const [qty, setQty] = useState<string>("1");
  const [toast, setToast] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: async () => {
      const n = Number(qty);
      if (!Number.isFinite(n) || n <= 0) throw new Error("수량을 입력하세요.");
      return side === "BUY" ? buyMarket(ticker, n) : sellMarket(ticker, n);
    },
    onSuccess: (r) => {
      setError(null);
      setToast(`${side === "BUY" ? "매수" : "매도"} 체결: ${r.quantity}주 · ${formatKrw(r.totalAmountKrw)}`);
      qc.invalidateQueries({ queryKey: ["portfolio"] });
      qc.invalidateQueries({ queryKey: ["history"] });
      setTimeout(() => setToast(null), 3000);
    },
    onError: (e) => {
      const ax = e as AxiosError<ApiError>;
      setError(ax.response?.data?.message ?? (e as Error).message ?? "주문 실패");
    },
  });

  const estimate = currentPriceKrw != null && Number(qty) > 0
    ? currentPriceKrw * Number(qty)
    : 0;

  return (
    <div style={styles.shell}>
      <div style={styles.tabs}>
        <button onClick={() => setSide("BUY")}  style={tabStyle(side === "BUY", "up")}>매수</button>
        <button onClick={() => setSide("SELL")} style={tabStyle(side === "SELL", "down")}>매도</button>
      </div>

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
        {[10, 25, 50, 100].map((pct) => (
          <button
            key={pct}
            onClick={() => {
              if (currentPriceKrw && side === "BUY") {
                // 시드머니 또는 잔고 정보 없이는 정확한 비율 매수 어려움. 단순 + 처리.
                setQty(String(pct));
              } else {
                setQty(String(pct));
              }
            }}
            style={styles.pctBtn}
          >+{pct}</button>
        ))}
      </div>

      <div style={styles.estimateBox}>
        <span style={styles.estimateLabel}>예상 체결금액</span>
        <span style={styles.estimateValue} className="tabular">{formatKrw(estimate)}</span>
      </div>

      {priceSourceHint && (
        <div style={styles.sourceHint}>
          💡 {priceSourceHint} 기준으로 모의 체결됩니다 (장 외 시간)
        </div>
      )}

      {error && <div style={styles.error}>{error}</div>}
      {toast && <div style={styles.toast}>{toast}</div>}

      <button
        onClick={() => mutation.mutate()}
        disabled={mutation.isPending || currentPriceKrw == null}
        style={{
          ...styles.submitBtn,
          background: side === "BUY" ? "var(--color-up)" : "var(--color-down)",
        }}
      >
        {mutation.isPending ? "처리 중..." : `${side === "BUY" ? "매수" : "매도"} 주문`}
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
  row: { display: "flex", alignItems: "center", gap: 12 },
  label: { fontSize: 13, color: "var(--text-secondary)", width: 48 },
  input: {
    flex: 1, height: 42, padding: "0 14px",
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
  toast: {
    fontSize: 12, color: "var(--color-success)",
    background: "rgba(22, 163, 74, 0.10)", padding: "8px 12px",
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
