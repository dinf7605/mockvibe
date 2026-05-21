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

export async function buyMarket(ticker: string, quantity: number): Promise<OrderResponse> {
  const res = await api.post<OrderResponse>("/trades/buy", { ticker, quantity });
  return res.data;
}

export async function sellMarket(ticker: string, quantity: number): Promise<OrderResponse> {
  const res = await api.post<OrderResponse>("/trades/sell", { ticker, quantity });
  return res.data;
}

export async function getHistory(page = 0, size = 20): Promise<TradeHistoryResponse> {
  const res = await api.get<TradeHistoryResponse>("/trades/history", { params: { page, size } });
  return res.data;
}
