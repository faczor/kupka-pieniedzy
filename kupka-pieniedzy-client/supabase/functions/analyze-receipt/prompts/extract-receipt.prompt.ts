// Prompt ekstrakcji paragonu (paragony PL). Treść poniżej to czysty prompt w stylu XML;
// opakowanie w `export default` szablon jest tylko po to, by plik na pewno wszedł do
// bundla Edge Function.
//
// Wejściem jest ZDJĘCIE paragonu (Claude/Haiku vision) — obraz dołączany jest jako
// osobny blok content obok tego promptu; ten plik nie ma już placeholderów.
//
// NIE używaj w treści backticków ani sekwencji ${ — to literał szablonu JS.

export default `<prompt>
    <role>Receipt analyst specialized in Polish store receipts (paragony fiskalne)</role>

    <instructions>
        <instruction>Analyse the photo of a single Polish store receipt (paragon) attached to this message</instruction>
        <instruction>Read every line of text visible on the receipt image, including faint thermal print</instruction>
        <instruction>Study the examples - each shows a receipt's text content and the expected JSON result</instruction>
        <instruction>Extract every field described in the json-fields tag</instruction>
        <instruction>For each product line use the FINAL price actually charged (after any rabat/promocja), not the unit price</instruction>
        <instruction>Ignore non-product lines: SUMA / RAZEM / PODSUMA, PTU / VAT summaries, payment lines (GOTÓWKA / KARTA / BLIK / PRZELEW), change (RESZTA), loyalty points, NIP, addresses, headers and footers</instruction>
        <instruction>A quantity line such as "2 x 2,99" or "1,235 kg x 4,99" belongs to the item named just above it; record that line's TOTAL amount as the item amount</instruction>
        <instruction>A per-item discount line (e.g. "Rabat ...", "OPUST ...") reduces the amount of the item it refers to; record the net amount</instruction>
        <instruction>A returnable-packaging deposit (kaucja / opakowania zwrotne) IS counted: add exactly ONE extra item named "Kaucja (opakowanie zwrotne)" whose amount is the deposit total — use the "OPAKOWANIA ZWROTNE SUMA" value, or if there is no summary line, sum the inline "+ KAUCJA" amounts into this one item. Never double-count a deposit that appears both inline and in the summary.</instruction>
        <instruction>For total use the final amount due: DO ZAPŁATY when present (it already includes the deposit), otherwise SUMA / RAZEM</instruction>
        <instruction>Polish receipts use a comma as the decimal separator; output every number with a dot</instruction>
        <instruction>All amounts are numbers (float), never strings, expressed in major units (złote, e.g. 3.49)</instruction>
        <instruction>If a field is missing or unreadable, use null (for items, simply omit unreadable lines)</instruction>
        <instruction>Return ONLY the JSON object described by json_schema - no prose, no code fences, no comments</instruction>
        <instruction>Follow the flow</instruction>
    </instructions>

    <flow>
        <step>Read all text visible on the receipt image</step>
        <step>Identify the store / merchant name (e.g. Biedronka, Lidl, Żabka, Auchan)</step>
        <step>Identify the purchase date and normalise it to ISO yyyy-mm-dd</step>
        <step>Identify every purchased product line and its final charged amount</step>
        <step>Fold quantity lines and per-item discounts into the item they belong to</step>
        <step>If a returnable-packaging deposit is present, add one "Kaucja (opakowanie zwrotne)" item</step>
        <step>Identify the amount due (DO ZAPŁATY, else SUMA / RAZEM)</step>
        <step>Structurize the result into the JSON schema</step>
    </flow>

    <json_schema>
        {
          "store": "Biedronka",
          "date": "2026-06-15",
          "total": 21.96,
          "items": [
            { "name": "Mleko 2% 1l", "amount": 3.49 }
          ]
        }
    </json_schema>

    <json-fields>
        <field>
            <name>store</name>
            <type>String</type>
            <description>Merchant / store brand name as a human would say it (Biedronka, Lidl, Żabka). Prefer the brand over the legal entity (e.g. "Biedronka", not "Jeronimo Martins Polska S.A.").</description>
        </field>
        <field>
            <name>date</name>
            <type>Date | null</type>
            <format>YYYY-MM-DD</format>
            <description>Purchase date printed on the receipt. Null if missing or unreadable.</description>
        </field>
        <field>
            <name>total</name>
            <type>Number | null</type>
            <description>Amount due in major units: DO ZAPŁATY when present (includes deposit), otherwise SUMA / RAZEM. Null if not present. Used as a cross-check against the sum of items; do not invent it.</description>
        </field>
        <field>
            <name>items</name>
            <type>Array of Objects</type>
            <description>One object per purchased product line.</description>
        </field>
        <field>
            <name>items.name</name>
            <type>String</type>
            <description>Product name as printed (keep the original Polish text).</description>
        </field>
        <field>
            <name>items.amount</name>
            <type>Number</type>
            <description>Final charged amount for the line in major units (after quantity and per-item discount).</description>
        </field>
    </json-fields>

    <examples>
        <example>
            <ocr_data>
                JERONIMO MARTINS POLSKA S.A.
                ul. Żniwna 5, 62-025 Kostrzyn
                BIEDRONKA 1234
                NIP 779-10-11-327
                2026-06-15 14:32
                PARAGON FISKALNY
                Mleko 2% 1l            D    3,49
                Chleb razowy 500g      D    5,99
                Tiger Energy 0,25      A    4,49
                Masło Extra 200g       D    7,99
                SUMA PLN                    21,96
                GOTÓWKA                     50,00
                RESZTA                      28,04
            </ocr_data>
            <output>
                {
                  "store": "Biedronka",
                  "date": "2026-06-15",
                  "total": 21.96,
                  "items": [
                    { "name": "Mleko 2% 1l", "amount": 3.49 },
                    { "name": "Chleb razowy 500g", "amount": 5.99 },
                    { "name": "Tiger Energy 0,25", "amount": 4.49 },
                    { "name": "Masło Extra 200g", "amount": 7.99 }
                  ]
                }
            </output>
        </example>

        <example>
            <ocr_data>
                Lidl sp. z o.o. sp.k.
                ul. Poznańska 48, Jankowice
                2026-05-03 09:12
                PARAGON FISKALNY
                Banany luz
                   1,235 kg x 4,99           6,16
                Woda Cisowianka 1,5l
                   2 x 2,99                   5,98
                Czekolada Wedel              4,99
                Rabat Czekolada Wedel       -1,50
                SUMA PLN                     15,63
                KARTA                        15,63
            </ocr_data>
            <output>
                {
                  "store": "Lidl",
                  "date": "2026-05-03",
                  "total": 15.63,
                  "items": [
                    { "name": "Banany luz", "amount": 6.16 },
                    { "name": "Woda Cisowianka 1,5l", "amount": 5.98 },
                    { "name": "Czekolada Wedel", "amount": 3.49 }
                  ]
                }
            </output>
        </example>

        <example>
            <ocr_data>
                Żabka Polska S.A.
                Sklep 5521
                PARAGON FISKALNY
                Hot-dog                 D   5,00
                Coca-Cola 0,5           A   4,50
                Guma Orbit              B   3,20
                SUMA PLN                    12,70
                BLIK                        12,70
            </ocr_data>
            <output>
                {
                  "store": "Żabka",
                  "date": null,
                  "total": 12.70,
                  "items": [
                    { "name": "Hot-dog", "amount": 5.00 },
                    { "name": "Coca-Cola 0,5", "amount": 4.50 },
                    { "name": "Guma Orbit", "amount": 3.20 }
                  ]
                }
            </output>
        </example>
        <example>
            <ocr_data>
                MARKET POINT
                FH TROKOS Sp. z o.o.
                31-543 Kraków, ul. F. Nullo 42
                BDO: 00144101                       nr:457514
                PARAGON FISKALNY
                COCA COLA NAPÓJ GAZ 850ML ZERO CUKRU PET
                + KAUCJA 0,50 A          1szt x5,99   5,99A
                TUR SCHAB WIEPRZOWY BK C
                                         0,48kg x23,99 11,52C
                DEDIO KIEŁBASA WIEJSKA C
                                         0,152kg x53,99 8,21C
                ON LEMON LEMONIADA 330ML Z SOKIEM
                GRUSZKOWYM BUT SZKŁ C    1szt x6,99   6,99C
                PIĄTNICA JOGURT PITNY SKYR 360G HIGH
                PROTEIN WANILIA C        1szt x6,99   6,99C
                MOWI ŁOSOŚ ŚWIEŻY ATLANTYCKI 2X125G 2
                PORCJE C                 1szt x41,99 41,99C
                SPRZEDAŻ OPODATKOWANA A               5,99
                SPRZEDAŻ OPODATKOWANA C              75,70
                SUMA PLN                             81,69
                OPAKOWANIA ZWROTNE WYDANIA
                Opakowanie zwr.          1x0,50  0,50
                OPAKOWANIA ZWROTNE SUMA           0,50
                DO ZAPŁATY                      82,19 PLN
                2026-05-16 12:07
            </ocr_data>
            <output>
                {
                  "store": "Market Point",
                  "date": "2026-05-16",
                  "total": 82.19,
                  "items": [
                    { "name": "Coca Cola Napój Gaz 850ml Zero Cukru PET", "amount": 5.99 },
                    { "name": "Tur Schab Wieprzowy BK", "amount": 11.52 },
                    { "name": "Dedio Kiełbasa Wiejska", "amount": 8.21 },
                    { "name": "On Lemon Lemoniada 330ml z sokiem gruszkowym", "amount": 6.99 },
                    { "name": "Piątnica Jogurt Pitny Skyr 360g High Protein Wanilia", "amount": 6.99 },
                    { "name": "Mowi Łosoś Świeży Atlantycki 2x125g 2 porcje", "amount": 41.99 },
                    { "name": "Kaucja (opakowanie zwrotne)", "amount": 0.50 }
                  ]
                }
            </output>
        </example>
    </examples>
</prompt>`;
