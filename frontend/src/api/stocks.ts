import { api } from "./client";

export interface StockItem {
  ticker: string;
  market: string;
  currency: string;
  companyName: string;
  sector: string | null;
  region: string | null;
  currentPrice: number | null;
  tickSize: number;
  isActive: boolean;
}

export interface StockSearchResponse {
  items: StockItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export async function searchStocks(params: {
  q?: string;
  market?: string;
  page?: number;
  size?: number;
}): Promise<StockSearchResponse> {
  const res = await api.get<StockSearchResponse>("/stocks/search", { params });
  return res.data;
}

export async function getStock(ticker: string): Promise<StockItem> {
  const res = await api.get<StockItem>(`/stocks/${ticker}`);
  return res.data;
}

/** 일봉 OHLCV — 차트 표시용. lightweight-charts CandlestickData 호환 */
export interface DailyCandle {
  time: string;   // 'YYYY-MM-DD'
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export async function getPriceHistory(
  ticker: string,
  days: number = 90,
): Promise<DailyCandle[]> {
  const res = await api.get<DailyCandle[]>(`/stocks/${ticker}/history`, {
    params: { days },
  });
  return res.data;
}

/** 가장 최근 종가 — 매매 패널의 '종가 기준' 안내용 */
export interface LastClose {
  ticker: string;
  tradeDate: string;
  close: number;
}

export async function getLastClose(ticker: string): Promise<LastClose> {
  const res = await api.get<LastClose>(`/stocks/${ticker}/last-close`);
  return res.data;
}
