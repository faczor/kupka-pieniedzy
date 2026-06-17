// Kontrakt input/output funkcji `analyze-receipt`.
// Kwoty na wyjściu są w jednostkach podrzędnych (grosze) — spójnie z klientem
// (com.sd.kupka_pieniedzy_client.core.money.Money) i `RawOcrJson`.

/** Żądanie do funkcji. Podaj `imagePath` (obiekt w buckecie Storage) LUB `imageBase64`. */
export interface AnalyzeRequest {
  /** Ścieżka obiektu w prywatnym buckecie Storage (np. "userId/receiptId.jpg"). */
  imagePath?: string;
  /** Alternatywa do testów: surowy base64 obrazu (z lub bez prefiksu data:). */
  imageBase64?: string;
  /** Nadpisanie nazwy bucketu; domyślnie env RECEIPTS_BUCKET lub "receipts". */
  bucket?: string;
  /** Kandydaci kategorii — NAZWY (po polsku), dokładnie jak w `categories.name`. */
  categories: string[];
  /** Kod waluty; domyślnie "PLN". */
  currency?: string;
}

/** Pojedyncza pozycja wyniku — gotowa do zbudowania RawAnalyzedItem po stronie klienta. */
export interface AnalyzedItem {
  name: string;
  /** Grosze (minor units), liczba całkowita. */
  amountMinor: number;
  /** Nazwa kategorii z listy `categories` albo null, gdy model nie był pewny. */
  suggestedCategory: string | null;
}

/** Odpowiedź funkcji — mapuje się 1:1 na domenowe RawReceiptAnalysis. */
export interface AnalyzeResponse {
  store: string;
  /** ISO yyyy-mm-dd albo null, gdy nieczytelna na paragonie. */
  date: string | null;
  currency: string;
  /** Niezmiennik kontraktu: totalMinor == suma items.amountMinor. */
  totalMinor: number;
  /** 0..1 — heurystyka pewności (zgodność sumy + odsetek skategoryzowanych). */
  confidence: number;
  items: AnalyzedItem[];
}

/** Pośredni wynik strukturyzacji z modelu — kwoty w jednostkach głównych (złote). */
export interface ExtractedReceipt {
  store: string;
  date: string | null;
  /** Suma z paragonu w jednostkach głównych (np. 56.84) albo null. */
  total: number | null;
  items: { name: string; amount: number }[];
}

/** Ustandaryzowany błąd domenowy zwracany w body odpowiedzi. */
export interface ApiError {
  error: { code: string; message: string };
}
