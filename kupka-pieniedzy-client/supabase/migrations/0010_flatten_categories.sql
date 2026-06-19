-- Spłaszczenie taksonomii kategorii: porzucamy dwupoziomowy model (L1 + sub-kategorie
-- spożywki L2). Cofa część D7 — patrz nowy wpis w docs/decisions.md. Dotychczasowe
-- sub-kategorie spożywki (energetyki, napoje, słodycze, jedzenie podstawowe, chemia,
-- alkohol) stają się zwykłymi, równorzędnymi kategoriami. Budżety i przypisania pozycji
-- (receipt_items.category_id) działają bez zmian — referują po `id`, nie po poziomie.

-- Indeks na parent_id znika razem z kolumną; dla jasności usuwamy go jawnie.
drop index if exists idx_categories_parent;

alter table categories drop column if exists parent_id;
alter table categories drop column if exists level;
alter table categories drop column if exists is_dynamic;
