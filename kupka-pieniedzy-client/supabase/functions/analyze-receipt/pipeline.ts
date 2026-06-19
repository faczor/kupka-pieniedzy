// FLOW analizy paragonu — czytaj krok po kroku. Każdy krok to osobny moduł w steps/.
import type { AnalyzeRequest, ReceiptAnalysis } from "./model.ts";
import { loadImage } from "./steps/load-image.ts";
import { loadUserContext } from "./steps/load-context.ts";
import { readReceipt } from "./steps/read-receipt.ts";
import { categorize } from "./steps/categorize.ts";
import { assemble } from "./steps/assemble.ts";
import { notReceipt } from "./errors.ts";
import { log } from "./log.ts";

export async function analyzeReceipt(req: AnalyzeRequest): Promise<ReceiptAnalysis> {
  const image = await loadImage(req); //                      1. obraz paragonu
  log.info("Step 1/5 image loaded", { mediaType: image.mediaType, base64Len: image.base64.length });

  const context = await loadUserContext(req); //              2. kategorie + historia usera
  log.info("Step 2/5 context loaded", {
    categories: context.categories.length,
    history: context.history.length,
  });

  const receipt = await readReceipt(image); //                3. odczyt ze zdjęcia (vision)
  log.info("Step 3/5 receipt read", {
    store: receipt.store,
    lines: receipt.lines.length,
    error: receipt.error,
  });

  // „To nie paragon”: model jawnie zgłosił błąd albo nie odczytał żadnej pozycji (rozmazane/nie-paragon).
  if (receipt.error || receipt.lines.length === 0) {
    throw notReceipt(
      `No usable receipt extracted (modelError=${receipt.error ?? "none"}, lines=${receipt.lines.length})`,
    );
  }

  const categories = await categorize(receipt, context); //   4. kategoryzacja pozycji
  log.info("Step 4/5 categorized", { items: categories.length });

  return assemble(receipt, categories, req.currency ?? "PLN"); // 5. złożenie (suma, confidence)
}
