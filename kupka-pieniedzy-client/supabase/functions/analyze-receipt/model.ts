// Model domenowy funkcji analizy paragonu. Bez logiki — same typy i stałe domenowe.

/** Catch-all: nieprzypisane pozycje lądują tutaj (seed: kategoria L1 "inne"). */
export const FALLBACK_CATEGORY = "inne";

/** Żądanie HTTP. Obraz: `imagePath` (Storage) LUB `imageBase64`. */
export interface AnalyzeRequest {
  imagePath?: string;
  imageBase64?: string;
  bucket?: string;
  /** Funkcja po nim czyta kategorie i pamięć. Wymagane, chyba że podasz `categories`. */
  userId?: string;
  currency?: string;
  /** Override kategorii (test/curl) — pomija odczyt z DB. */
  categories?: string[];
  /** Override historii few-shot (test/curl) — pomija odczyt product_categories. */
  examples?: CategoryExample[];
}

/** Obraz paragonu gotowy dla Claude vision. */
export interface ReceiptImage {
  base64: string;
  mediaType: string;
}

/** Para nazwa→kategoria: historia kategoryzacji usera (few-shot). */
export interface CategoryExample {
  name: string;
  category: string;
}

/** Kontekst usera potrzebny do kategoryzacji. */
export interface UserContext {
  categories: string[];
  history: CategoryExample[];
}

/** Pojedyncza pozycja odczytana ze zdjęcia (kwota w jednostkach głównych, złote). */
export interface ReceiptLine {
  name: string;
  amount: number;
}

/** Wynik Etapu A (odczyt ze zdjęcia). */
export interface ReadReceipt {
  store: string;
  date: string | null;
  /** Kwota do zapłaty (DO ZAPŁATY/SUMA) w złotych — tylko do cross-checku. */
  printedTotal: number | null;
  lines: ReceiptLine[];
}

/** Pozycja w odpowiedzi (kwota w groszach, spójnie z Money/raw_ocr_json). */
export interface AnalyzedItem {
  name: string;
  amountMinor: number;
  suggestedCategory: string | null;
}

/** Linia surowego odczytu (sprzed kategoryzacji), kwota w groszach. */
export interface RawReadLine {
  name: string;
  amountMinor: number;
}

/**
 * Surowy odczyt paragonu sprzed kategoryzacji — artefakt analityczny.
 * Klient zapisuje go do `receipts.raw_ocr_json` (audyt), ale UI go nie czyta.
 */
export interface RawRead {
  store: string;
  date: string | null;
  /** Kwota „DO ZAPŁATY/SUMA” z paragonu (cross-check) w groszach. */
  printedTotalMinor: number | null;
  lines: RawReadLine[];
}

/** Odpowiedź funkcji — mapuje się 1:1 na domenowe RawReceiptAnalysis. */
export interface ReceiptAnalysis {
  store: string;
  date: string | null;
  currency: string;
  /** Niezmiennik: totalMinor == suma items.amountMinor. */
  totalMinor: number;
  confidence: number;
  items: AnalyzedItem[];
  /** Surowy odczyt sprzed kategoryzacji (do raw_ocr_json, tylko audyt/analiza). */
  raw: RawRead;
}
