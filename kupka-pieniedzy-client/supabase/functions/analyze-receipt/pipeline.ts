// FLOW analizy paragonu — czytaj krok po kroku. Każdy krok to osobny moduł w steps/.
import type { AnalyzeRequest, ReceiptAnalysis } from "./model.ts";
import { loadImage } from "./steps/load-image.ts";
import { loadUserContext } from "./steps/load-context.ts";
import { readReceipt } from "./steps/read-receipt.ts";
import { categorize } from "./steps/categorize.ts";
import { assemble } from "./steps/assemble.ts";

export async function analyzeReceipt(req: AnalyzeRequest): Promise<ReceiptAnalysis> {
  const image = await loadImage(req); //                      1. obraz paragonu
  const context = await loadUserContext(req); //              2. kategorie + historia usera
  const receipt = await readReceipt(image); //                3. odczyt ze zdjęcia (vision)
  const categories = await categorize(receipt, context); //   4. kategoryzacja pozycji
  return assemble(receipt, categories, req.currency ?? "PLN"); // 5. złożenie (suma, confidence)
}
