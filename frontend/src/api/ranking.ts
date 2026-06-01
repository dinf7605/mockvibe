import { api } from "./client";

export interface RankingEntry {
  rank: number;
  username: string;
  returnPct: number;
  totalAssetKrw: number;
}

export interface RankingResponse {
  entries: RankingEntry[];
  asOf: string | null;
  myRank: number | null;
  myReturnPct: number | null;
  totalParticipants: number;
}

export interface TrendPoint {
  date: string;
  totalAssetKrw: number;
  returnPct: number;
}

export async function getRanking(limit = 20): Promise<RankingResponse> {
  const res = await api.get<RankingResponse>("/ranking", { params: { limit } });
  return res.data;
}

export async function getMyTrend(): Promise<TrendPoint[]> {
  const res = await api.get<TrendPoint[]>("/ranking/me/trend");
  return res.data;
}
