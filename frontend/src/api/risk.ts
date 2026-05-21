import { api } from "./client";

export interface RiskResponse {
  var95: number;
  var99: number;
  sharpe: number;
  beta: number;
  mdd: number;
  concentration: number;
  regionShare: Record<string, number>;
  sectorShare: Record<string, number>;
  warnings: string[];
}

export async function getRisk(): Promise<RiskResponse> {
  const res = await api.get<RiskResponse>("/risk");
  return res.data;
}
