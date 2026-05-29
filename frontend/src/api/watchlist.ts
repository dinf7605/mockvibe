import { api } from "./client";
import type { StockItem } from "./stocks";

/** 내 관심종목 목록 (종목 정보 포함). */
export async function getWatchlist(): Promise<StockItem[]> {
  const res = await api.get<StockItem[]>("/watchlist");
  return res.data;
}

/** 특정 종목이 내 관심종목인지 여부. */
export async function isWatching(ticker: string): Promise<boolean> {
  const res = await api.get<{ ticker: string; watching: boolean }>(
    `/watchlist/${ticker}/contains`,
  );
  return res.data.watching;
}

/** 관심종목 추가 (멱등). */
export async function addWatchlist(ticker: string): Promise<void> {
  await api.post(`/watchlist/${ticker}`);
}

/** 관심종목 제거 (멱등). */
export async function removeWatchlist(ticker: string): Promise<void> {
  await api.delete(`/watchlist/${ticker}`);
}
