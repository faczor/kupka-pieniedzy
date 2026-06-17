// Infra: dostęp do Supabase service-rolem — pobranie obrazu ze Storage,
// odczyt kategorii usera i pamięci kategoryzacji (product_categories).
import { createClient, type SupabaseClient } from "npm:@supabase/supabase-js@2";
import { encodeBase64 } from "jsr:@std/encoding@1/base64";
import type { CategoryExample } from "../model.ts";
import { internal, invalidRequest } from "../errors.ts";

function serviceClient(): SupabaseClient {
  const url = Deno.env.get("SUPABASE_URL");
  const key = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !key) throw internal("Missing SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY");
  return createClient(url, key);
}

/** Pobiera obraz z prywatnego bucketu i zwraca base64. */
export async function downloadImageBase64(bucket: string, path: string): Promise<string> {
  const { data, error } = await serviceClient().storage.from(bucket).download(path);
  if (error || !data) {
    throw invalidRequest(
      `Cannot download image '${path}' from bucket '${bucket}': ${error?.message ?? "not found"}`,
    );
  }
  return encodeBase64(new Uint8Array(await data.arrayBuffer()));
}

/** Wszystkie NAZWY kategorii usera (L1 + L2) — pełna lista dla modelu. */
export async function fetchCategories(userId: string): Promise<string[]> {
  const { data, error } = await serviceClient()
    .from("categories")
    .select("name")
    .eq("user_id", userId);
  if (error) throw internal(`categories query failed: ${error.message}`);
  return (data ?? [])
    .map((r) => String((r as { name?: unknown }).name ?? "").trim())
    .filter((n) => n.length > 0);
}

/**
 * Historia kategoryzacji: do `maxDistinct` UNIKALNYCH par nazwa→kategoria,
 * najświeższe pierwsze (deduplikacja po znormalizowanej nazwie).
 */
export async function fetchMemory(userId: string, maxDistinct = 50): Promise<CategoryExample[]> {
  const { data, error } = await serviceClient()
    .from("product_categories")
    .select("product_name, created_at, categories(name)")
    .eq("user_id", userId)
    .order("created_at", { ascending: false })
    .limit(400);
  if (error) throw internal(`product_categories query failed: ${error.message}`);

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
