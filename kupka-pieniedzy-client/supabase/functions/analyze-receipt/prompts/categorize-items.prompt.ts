// Prompt kategoryzacji pozycji paragonu. Treść poniżej to czysty prompt w stylu XML;
// opakowanie w export default jest po to, by plik wszedł do bundla Edge Function.
// Placeholdery {{...}} podmienia render() z ../services/prompt.ts.
//
// Zmienne:
//   {{USER_HISTORY}} — historia kategoryzacji usera ("nazwa => kategoria" po jednej w linii)
//   {{CATEGORIES}}   — dozwolone NAZWY kategorii (po jednej w linii), z categories.name usera
//   {{ITEMS}}        — pozycje do skategoryzowania, format "<index>. <nazwa>" po jednej w linii
//
// NIE używaj w treści backticków ani sekwencji ${.

export default `<prompt>
    <role>Budget categorizer for Polish grocery receipt line items</role>

    <instructions>
        <instruction>Assign each item from the items tag to exactly one category from the categories tag</instruction>
        <instruction>Study the examples - they show items and the expected assignments</instruction>
        <instruction>Match by what the product actually is, not by brand alone (an energy drink belongs to energetyki even if branded)</instruction>
        <instruction>The user_history tag lists how THIS user categorized items in the past, including their manual corrections - treat it as the STRONGEST signal: if an item matches or closely resembles a user_history entry, follow that categorization even over your own intuition</instruction>
        <instruction>Use a category string ONLY if it appears verbatim in the categories tag</instruction>
        <instruction>When no specific category clearly fits, use the catch-all category "inne" if it is present in the list; only use null if there is no catch-all category</instruction>
        <instruction>A returnable-packaging deposit line ("Kaucja (opakowanie zwrotne)") belongs to the catch-all "inne"</instruction>
        <instruction>When genuinely unsure, prefer the catch-all "inne" over guessing a specific category</instruction>
        <instruction>Return exactly one assignment per input item, keyed by its index</instruction>
        <instruction>Return ONLY the JSON object described by json_schema - no prose, no code fences, no comments</instruction>
    </instructions>

    <json_schema>
        {
          "assignments": [
            { "index": 0, "category": "jedzenie podstawowe" }
          ]
        }
    </json_schema>

    <json-fields>
        <field>
            <name>assignments</name>
            <type>Array of Objects</type>
            <description>One object per input item.</description>
        </field>
        <field>
            <name>assignments.index</name>
            <type>Number</type>
            <description>0-based index of the item, exactly as given in the items tag.</description>
        </field>
        <field>
            <name>assignments.category</name>
            <type>String | null</type>
            <description>One category name copied verbatim from the categories tag. Use the catch-all "inne" when nothing specific fits (if present); null only when no catch-all exists.</description>
        </field>
    </json-fields>

    <examples>
        <example>
            <categories>
                jedzenie podstawowe
                energetyki
                napoje
                słodycze
                chemia
                alkohol
                inne
            </categories>
            <items>
                0. Mleko 2% 1l
                1. Tiger Energy 0,25
                2. Papier Velvet 8szt
                3. Czekolada Wedel
                4. Woda Cisowianka 1,5l
                5. Kaucja (opakowanie zwrotne)
                6. Zgrzew tajemniczy XYZ
            </items>
            <output>
                {
                  "assignments": [
                    { "index": 0, "category": "jedzenie podstawowe" },
                    { "index": 1, "category": "energetyki" },
                    { "index": 2, "category": "chemia" },
                    { "index": 3, "category": "słodycze" },
                    { "index": 4, "category": "napoje" },
                    { "index": 5, "category": "inne" },
                    { "index": 6, "category": "inne" }
                  ]
                }
            </output>
        </example>
    </examples>

    <user_history>
{{USER_HISTORY}}
    </user_history>

    <categories>
{{CATEGORIES}}
    </categories>

    <items>
{{ITEMS}}
    </items>
</prompt>`;
