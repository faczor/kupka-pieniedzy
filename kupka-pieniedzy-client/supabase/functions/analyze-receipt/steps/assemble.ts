// Krok 5: złożenie wyniku. Kwoty -> grosze, total = suma pozycji (niezmiennik),
// confidence z heurystyki (zgodność z paragonem + odsetek skategoryzowanych).
import {
  FALLBACK_CATEGORY,
  type AnalyzedItem,
  type ReadReceipt,
  type ReceiptAnalysis,
} from "../model.ts";

export function assemble(
  receipt: ReadReceipt,
  categories: (string | null)[],
  currency: string,
): ReceiptAnalysis {
  const items: AnalyzedItem[] = receipt.lines.map((line, i) => ({
    name: line.name,
    amountMinor: Math.round(line.amount * 100),
    suggestedCategory: categories[i] ?? null,
  }));

  const totalMinor = items.reduce((sum, it) => sum + it.amountMinor, 0);
  const printedMinor = receipt.printedTotal !== null
    ? Math.round(receipt.printedTotal * 100)
    : null;

  return {
    store: receipt.store,
    date: receipt.date,
    currency,
    totalMinor,
    confidence: confidence(printedMinor, totalMinor, items),
    items,
  };
}

/** 0..1: zgodność sumy z paragonem + odsetek pozycji z konkretną (nie-fallback) kategorią. */
function confidence(
  printedMinor: number | null,
  sumMinor: number,
  items: AnalyzedItem[],
): number {
  const specific = items.length
    ? items.filter((i) => i.suggestedCategory && i.suggestedCategory !== FALLBACK_CATEGORY)
        .length / items.length
    : 0;
  let c = 0.5 + 0.4 * specific; // 0.5..0.9
  const totalsMatch = printedMinor === null || Math.abs(printedMinor - sumMinor) <= 2;
  if (!totalsMatch) c -= 0.25;
  return Math.max(0.1, Math.min(0.97, Number(c.toFixed(2))));
}
