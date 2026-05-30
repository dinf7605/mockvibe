import { api } from "./client";

export type OrderSide = "BUY" | "SELL";
export type OrderMethod = "MARKET" | "LIMIT";

export interface OrderResponse {
  orderId: number;
  ticker: string;
  orderType: OrderSide;
  orderMethod: OrderMethod;
  price: number;
  quantity: number;
  fxRate: number;
  fee: number;
  totalAmountKrw: number;
  walletBalanceAfterKrw: number;
  createdAt: string;
}

export interface TradeHistoryItem {
  orderId: number;
  ticker: string;
  orderType: OrderSide;
  orderMethod: OrderMethod;
  price: number;
  quantity: number;
  fxRate: number;
  fee: number;
  totalAmountKrw: number;
  createdAt: string;
}

export interface TradeHistoryResponse {
  items: TradeHistoryItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** 매 주문 제출마다 새 키 — 재시도·더블클릭 시 같은 키가 재사용되어 서버에서 중복 차단. */
function newIdempotencyKey(): string {
  return typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export async function buyMarket(
  ticker: string,
  quantity: number,
  idempotencyKey: string = newIdempotencyKey(),
): Promise<OrderResponse> {
  const res = await api.post<OrderResponse>(
    "/trades/buy",
    { ticker, quantity },
    { headers: { "Idempotency-Key": idempotencyKey } },
  );
  return res.data;
}

export async function sellMarket(
  ticker: string,
  quantity: number,
  idempotencyKey: string = newIdempotencyKey(),
): Promise<OrderResponse> {
  const res = await api.post<OrderResponse>(
    "/trades/sell",
    { ticker, quantity },
    { headers: { "Idempotency-Key": idempotencyKey } },
  );
  return res.data;
}

export async function getHistory(page = 0, size = 20): Promise<TradeHistoryResponse> {
  const res = await api.get<TradeHistoryResponse>("/trades/history", { params: { page, size } });
  return res.data;
}
