// Krok 2: kontekst usera potrzebny do kategoryzacji — kategorie + historia.
// Domyślnie z DB (po userId); override z requestu = tryb testowy bez DB.
import type { AnalyzeRequest, UserContext } from "../model.ts";
import { invalidRequest } from "../errors.ts";
import { fetchCategories, fetchMemory } from "../services/supabase.ts";

export async function loadUserContext(req: AnalyzeRequest): Promise<UserContext> {
  if (req.categories) {
    return { categories: req.categories, history: req.examples ?? [] };
  }
  if (!req.userId) {
    throw invalidRequest("Provide userId (or a categories override)");
  }
  const categories = await fetchCategories(req.userId);
  const history = req.examples ?? await fetchMemory(req.userId);
  return { categories, history };
}
