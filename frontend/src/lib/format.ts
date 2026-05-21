/** KRW를 원화 표기로 (1,234,567원). 음수는 -1,234원. */
export function formatKrw(n: number | string | null | undefined): string {
  if (n === null || n === undefined) return "-";
  const v = typeof n === "string" ? Number(n) : n;
  if (Number.isNaN(v)) return "-";
  return v.toLocaleString("ko-KR", { maximumFractionDigits: 0 }) + "원";
}

/** 등락률 표기 (+1.23% / -2.34% / 0.00%). 한국식 색상은 .up/.down 클래스로. */
export function formatPct(n: number | string | null | undefined): string {
  if (n === null || n === undefined) return "-";
  const v = typeof n === "string" ? Number(n) : n;
  if (Number.isNaN(v)) return "-";
  const sign = v > 0 ? "+" : "";
  return `${sign}${v.toFixed(2)}%`;
}

/** 0보다 큰지(상승)/작은지(하락) → CSS 클래스 ('up'/'down'/'flat') */
export function trendClass(n: number | string | null | undefined): "up" | "down" | "flat" {
  const v = typeof n === "string" ? Number(n) : (n ?? 0);
  if (Number.isNaN(v) || v === 0) return "flat";
  return v > 0 ? "up" : "down";
}
