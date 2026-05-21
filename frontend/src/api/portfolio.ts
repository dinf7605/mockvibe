import { api } from "./client";

export interface HoldingItem {
  ticker: string;
  companyName: string;
  market: string;
  currency: string;
  quantity: number;
  averagePriceKrw: number;
  currentPriceKrw: number;
  evaluationKrw: number;
  pnlKrw: number;
  pnlPct: number;
}

export interface RegionShare {
  kr: number;
  us: number;
  cash: number;
}

export interface PortfolioResponse {
  cashBalanceKrw: number;
  holdingValueKrw: number;
  totalAssetKrw: number;
  totalCostKrw: number;
  totalPnlKrw: number;
  totalPnlPct: number;
  holdings: HoldingItem[];
  regionShare: RegionShare;
}

export async function getPortfolio(): Promise<PortfolioResponse> {
  const res = await api.get<PortfolioResponse>("/portfolio");
  return res.data;
}
