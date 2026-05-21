import { api } from "./client";

export interface BacktestRequest {
  strategy: "BUY_AND_HOLD" | "MOVING_AVERAGE_20" | "RSI_14";
  ticker: string;
  startDate: string;     // YYYY-MM-DD
  endDate: string;
  initialCapital: number;
}

export interface BacktestResponse {
  runId: number;
  strategy: string;
  ticker: string;
  startDate: string;
  endDate: string;
  initialCapital: number;
  finalValue: number;
  totalReturn: number;
  mdd: number;
  sharpe: number;
  tradeCount: number;
  winRate: number;
  equityCurve: { date: string; equity: number }[];
  trades: { date: string; side: "BUY" | "SELL"; price: number; quantity: number }[];
}

export async function runBacktest(req: BacktestRequest): Promise<BacktestResponse> {
  const res = await api.post<BacktestResponse>("/backtest/run", req);
  return res.data;
}
