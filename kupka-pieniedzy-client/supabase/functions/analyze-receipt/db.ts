// Odczyt z Supabase (service role): kategorie usera + pamięć kategoryzacji.
// Pamięć to istniejąca tabela `product_categories` (nazwa produktu -> kategoria,
// source 'manual'|'llm', created_at). Pisaniem do niej zajmuje się klient przy
// zapisie/edycji paragonu — tutaj tylko czytamy.
import { createClient, type SupabaseClient } from "npm:@supabase/supabase-js@2";
import type { CategoryExample } from "./types.ts";

export function serviceClient(): SupabaseClient {
  const url = Deno.env.get("SUPABASE_URL");
  const key = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !key) {
    throw new Error("Missing SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY");
  }
  return createClient(url, key);
}

/** Wszystkie NAZWY kategorii usera (L1 + L2) — pełna lista dostępna dla modelu. */
export async function fetchCategories(
  supabase: SupabaseClient,
  userId: string,
): Promise<string[]> {
  const { data, error } = await supabase
    .from("categories")
    .select("name")
    .eq("user_id", userId);
  if (error) throw new Error(`categories query failed: ${error.message}`);
  return (data ?? [])
    .map((r) => String((r as { name?: unknown }).name ?? "").trim())
    .filter((n) => n.length > 0);
}

/**
 * Historia kategoryzacji usera: do `maxDistinct` UNIKALNYCH par nazwa→kategoria,
 * najświeższe pierwsze (deduplikacja po znormalizowanej nazwie — ostatnia wygrywa).
 */
export async function fetchMemory(
  supabase: SupabaseClient,
  userId: string,
  maxDistinct = 50,
): Promise<CategoryExample[]> {
  const { data, error } = await supabase
    .from("product_categories")
    .select("product_name, created_at, categories(name)")
    .eq("user_id", userId)
    .order("created_at", { ascending: false })
    .limit(400);
  if (error) throw new Error(`product_categories query failed: ${error.message}`);

  const seen = new Set<string>();
  const out: CategoryExample[] = [];
  for (const row of data ?? []) {
    const r = row as { product_name?: unknown; categories?: unknown };
    const name = String(r.product_name ?? "").trim();
    const cat = r.categories as { name?: unknown } | { name?: unknown }[] | null;
    const category = Array.isArray(cat)
      ? String(cat[0]?.name ?? "").trim()
      : String(cat?.name ?? "").trim();
    if (!name || !category) continue;
    const key = name.toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    out.push({ name, category });
    if (out.length >= maxDistinct) break;
  }
  return out;
}
