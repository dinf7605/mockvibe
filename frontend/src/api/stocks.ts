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
