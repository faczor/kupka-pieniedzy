// Prompt kategoryzacji pozycji paragonu. Treść poniżej to czysty prompt w stylu XML;
// opakowanie w export default jest po to, by plik wszedł do bundla Edge Function.
// Placeholdery {{...}} podmienia render() z ../prompts.ts.
//
// Zmienne:
//   {{CATEGORIES}} — dozwolone NAZWY kategorii (po jednej w linii), z categories.name usera
//   {{ITEMS}}      — pozycje do skategoryzowania, format "<index>. <nazwa>" po jednej w linii
//
// NIE używaj w treści backticków ani sekwencji ${.

export default `<prompt>
    <role>Budget categorizer for Polish grocery receipt line items</role>

    <instructions>
        <instruction>Assign each item from the items tag to exactly one category from the categories tag, or null when none clearly fits</instruction>
        <instruction>Study the examples - they show items and the expected assignments</instruction>
        <instruction>Match by what the product actually is, not by brand alone (an energy drink belongs to energetyki even if branded)</instruction>
        <instruction>Use a category string ONLY if it appears verbatim in the categories tag; otherwise use null</instruction>
        <instruction>When genuinely unsure, return null rather than guessing</instruction>
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
            <description>One category name copied verbatim from the categories tag, or null.</description>
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
            </categories>
            <items>
                0. Mleko 2% 1l
                1. Tiger Energy 0,25
                2. Papier Velvet 8szt
                3. Czekolada Wedel
                4. Woda Cisowianka 1,5l
                5. Zgrzew tajemniczy XYZ
            </items>
            <output>
                {
                  "assignments": [
                    { "index": 0, "category": "jedzenie podstawowe" },
                    { "index": 1, "category": "energetyki" },
                    { "index": 2, "category": "chemia" },
                    { "index": 3, "category": "słodycze" },
                    { "index": 4, "category": "napoje" },
                    { "index": 5, "category": null }
                  ]
                }
            </output>
        </example>
    </examples>

    <categories>
{{CATEGORIES}}
    </categories>

    <items>
{{ITEMS}}
    </items>
</prompt>`;
