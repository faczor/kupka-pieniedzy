// Lekki logger funkcji — jeden tag, wychodzi do logów Supabase (Dashboard → Edge Functions → Logs).
// Trzymamy się console.*, bo to jedyne, co runtime Deno zbiera do logów funkcji.
const TAG = "[analyze-receipt]";

function fmt(data?: Record<string, unknown>): string {
  return data ? " " + JSON.stringify(data) : "";
}

export const log = {
  info: (msg: string, data?: Record<string, unknown>) => console.log(`${TAG} ${msg}${fmt(data)}`),
  warn: (msg: string, data?: Record<string, unknown>) => console.warn(`${TAG} ${msg}${fmt(data)}`),
  error: (msg: string, err?: unknown) => {
    const detail = err instanceof Error ? `${err.name}: ${err.message}` : err;
    console.error(`${TAG} ${msg}`, detail ?? "");
  },
};
