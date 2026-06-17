// Kontrakt input/output funkcji `analyze-receipt`.
// Kwoty na wyjściu są w jednostkach podrzędnych (grosze) — spójnie z klientem
// (com.sd.kupka_pieniedzy_client.core.money.Money) i `RawOcrJson`.

/** Para nazwa→kategoria: historia kategoryzacji usera (few-shot) i/lub override do testów. */
export interface CategoryExample {
  name: string;
  category: string;
}

/** Żądanie do funkcji. Podaj `imagePath` (obiekt w Storage) LUB `imageBase64`. */
export interface AnalyzeRequest {
  /** Ścieżka obiektu w prywatnym buckecie Storage (np. "userId/receiptId.jpg"). */
  imagePath?: string;
  /** Alternatywa do testów: surowy base64 obrazu (z lub bez prefiksu data:). */
  imageBase64?: string;
  /** Nadpisanie nazwy bucketu; domyślnie env RECEIPTS_BUCKET lub "receipts". */
  bucket?: string;
  /**
   * Id usera — funkcja po nim odpytuje kategorie i pamięć (product_categories).
   * Wymagane, chyba że podasz `categories` (tryb testowy bez DB).
   */
  userId?: string;
  /** Kod waluty; domyślnie "PLN". */
  currency?: string;
  /** Override listy kategorii (NAZWY). Gdy podany — pomija odczyt z DB (test/curl). */
  categories?: string[];
  /** Override historii few-shot. Gdy podany — pomija odczyt product_categories (test/curl). */
  examples?: CategoryExample[];
}

/** Pojedyncza pozycja wyniku — gotowa do zbudowania RawAnalyzedItem po stronie klienta. */
export interface AnalyzedItem {
  name: string;
  /** Grosze (minor units), liczba całkowita. */
  amountMinor: number;
  /** Nazwa kategorii z listy kategorii usera albo null. */
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
  /** Kwota do zapłaty w jednostkach głównych (DO ZAPŁATY / SUMA) albo null. */
  total: number | null;
  items: { name: string; amount: number }[];
}

/** Ustandaryzowany błąd domenowy zwracany w body odpowiedzi. */
export interface ApiError {
  error: { code: string; message: string };
}
