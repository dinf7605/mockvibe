import { api } from "./client";

export interface NewsItem {
  headline: string;
  source: string;
  summary: string;
  url: string;
  datetime: number; // unix epoch seconds
  image: string;
}

export async function getStockNews(ticker: string, days = 7): Promise<NewsItem[]> {
  const res = await api.get<NewsItem[]>(`/stocks/${ticker}/news`, { params: { days } });
  return res.data;
}
