/*
 * SearchEngine.java - the heart of the project.
 *
 * Every score below is COMPUTED from the query and the CSV data.
 * Nothing is hardcoded.
 *
 * Algorithms used here:
 *   inverted index lookup ...... universal hashing          (CO6)
 *   exact keyword match ........ KMP                        (CO2)
 *   brand / type match ......... Rabin-Karp rolling hash    (CO2)
 *   name prefix match .......... Z-function                 (CO2)
 *   typo tolerant match ........ Wagner-Fischer / Damerau   (CO3)
 *   description similarity ..... Suffix Array + Kasai LCP   (CO2)
 *   phrase alignment ........... Smith-Waterman             (CO3)
 *   ranking .................... Randomised QuickSort       (CO6)
 *   diversity filtering ........ Vertex Cover 2-approx      (CO5)
 *   budget bundle .............. 0/1 Knapsack DP            (CO3/CO5)
 *   aspect assignment .......... Bipartite matching/max-flow(CO4)
 */
public class SearchEngine {

    ProductRepository cat;
    Rand rand;

    SearchEngine(ProductRepository c, Rand r) { cat = c; rand = r; }

    /* --- result of scoring one product --- */
    static class Hit {
        int pos;              // index in catalog
        int score;
        int keywordHits;      // number of query keywords found exactly (KMP)
        int fuzzyHits;        // matched only after typo correction (edit distance)
        StrList matched = new StrList();
        StrList fuzzyWords = new StrList();
        boolean catMatch, typeMatch, brandMatch, nameMatch;
        boolean typeIntent;   // the query clearly names this product type
        int lcsLen;           // suffix array + LCP similarity
        int alignScore;       // Smith-Waterman
        boolean budgetOk;
        int budget;
        String verdict;       // YES / PARTIALLY / NO
    }

    /*
     * search: returns hits sorted best first.
     * category = null or "All Products" means no category restriction.
     */
    Hit[] search(String query, String category, int minPrice, int maxPrice, int limit) {
        StrList kw = Text.keywords(query);
        StrList wanted = wantedTypes(kw);                 // product types the requirement really asks for
        StrList qtok = Text.tokenize(query);
        boolean cheapWish = hasAny(qtok, "budget", "cheap", "affordable", "economical", "inexpensive");
        boolean premiumWish = hasAny(qtok, "premium", "luxury", "expensive", "costly");
        int budget = Text.extractBudget(query);
        if (budget > 0 && maxPrice <= 0) maxPrice = budget;

        /* [CO6] ---- step 1: gather candidates from the inverted index (universal hashing) ---- */
        boolean[] seen = new boolean[cat.count];
        IntList cand = new IntList();
        for (int i = 0; i < kw.size(); i++) {
            IntList lst = cat.index.get(kw.get(i));
            if (lst == null) continue;
            for (int j = 0; j < lst.size(); j++) {
                int p = lst.get(j);
                if (!seen[p]) { seen[p] = true; cand.add(p); }
            }
        }
        /* if a keyword was misspelled the index gives nothing -> fall back to whole category */
        if (wanted.size() > 0) {                          // type named: search only products of that type
            cand = new IntList(); seen = new boolean[cat.count];
            for (int i = 0; i < cat.count; i++)
                if (wanted.contains(cat.products[i].type.toLowerCase())) { seen[i] = true; cand.add(i); }
        }
        if (cand.size() < 10) {
            for (int i = 0; i < cat.count; i++) if (!seen[i]) { seen[i] = true; cand.add(i); }
        }

        /* ---- step 2: score every candidate ---- */
        /* price range of what survives the filters (used for "cheap" / "premium" wishes) */
        int lo = Integer.MAX_VALUE, hi = 0;
        for (int c = 0; c < cand.size(); c++) {
            Product q0 = cat.products[cand.get(c)];
            if (category != null && !category.equals("All Products") && !q0.category.equals(category)) continue;
            if (wanted.size() > 0 && !wanted.contains(q0.type.toLowerCase())) continue;
            if (minPrice > 0 && q0.price < minPrice) continue;
            if (maxPrice > 0 && q0.price > maxPrice) continue;
            if (q0.price < lo) lo = q0.price;
            if (q0.price > hi) hi = q0.price;
        }
        Hit[] tmp = new Hit[cand.size()];
        int[] scores = new int[cand.size()];
        int[] idxs = new int[cand.size()];
        int n = 0;

        for (int c = 0; c < cand.size(); c++) {
            int p = cand.get(c);
            Product pr = cat.products[p];

            if (category != null && !category.equals("All Products") && !pr.category.equals(category)) continue;
            if (wanted.size() > 0 && !wanted.contains(pr.type.toLowerCase())) continue;
            if (minPrice > 0 && pr.price < minPrice) continue;
            if (maxPrice > 0 && pr.price > maxPrice) continue;

            Hit h = scoreProduct(pr, p, kw, category, budget, query);
            if (wanted.size() > 0) { h.typeMatch = true; h.typeIntent = true; h.score += 30; }
            if (cheapWish && hi > lo) h.score += 40 * (hi - pr.price) / (hi - lo);        // cheaper = better
            if (premiumWish && hi > lo) h.score += 40 * (pr.price - lo) / (hi - lo);      // dearer = better
            if (h.score <= 0) continue;
            tmp[n] = h; scores[n] = h.score; idxs[n] = n; n++;
        }

        if (n == 0) return new Hit[0];

        /* ---- step 3: rank with randomised quicksort (CO6, expected O(n log n)) ---- */
        int[] s2 = new int[n], i2 = new int[n];
        for (int i = 0; i < n; i++) { s2[i] = scores[i]; i2[i] = i; }
        ParallelSearch.randomizedQuickSort(s2, i2, 0, n - 1, rand);

        int take = Math.min(limit, n);

        /* [CO2][CO3] ---- step 4: deep similarity only on the shortlist (expensive algorithms) ---- */
        Hit[] out = new Hit[take];
        for (int i = 0; i < take; i++) {
            Hit h = tmp[i2[i]];
            String q = query.toLowerCase();
            h.lcsLen = SimilaritySearch.longestCommonSubstring(q, pr(h).descLower);   // suffix array + Kasai
            h.alignScore = SimilaritySearch.smithWaterman(q, pr(h).descLower, 2, -1, -2); // local alignment
            h.score += h.lcsLen + h.alignScore / 4;
            h.verdict = verdict(h, kw.size());
            out[i] = h;
        }

        /* the deep-similarity pass changed the scores, so rank once more */
        int[] s3 = new int[take], i3 = new int[take];
        for (int i = 0; i < take; i++) { s3[i] = out[i].score; i3[i] = i; }
        ParallelSearch.randomizedQuickSort(s3, i3, 0, take - 1, rand);
        Hit[] sorted = new Hit[take];
        for (int i = 0; i < take; i++) sorted[i] = out[i3[i]];
        return sorted;
    }

    Product pr(Hit h) { return cat.products[h.pos]; }

    /* ---------- per product scoring ---------- */
    Hit scoreProduct(Product pr, int pos, StrList kw, String category, int budget, String rawQuery) {
        Hit h = new Hit();
        h.pos = pos;
        h.budget = budget;
        int score = 0;

        String blob = pr.descLower + " " + pr.nameLower + " " + pr.type.toLowerCase() + " " + pr.brand.toLowerCase();

        for (int i = 0; i < kw.size(); i++) {
            String w = kw.get(i);
            int occ = PatternSearch.kmpCount(blob, w);             // [CO2] KMP exact search
            if (occ > 0) {
                h.keywordHits++;
                h.matched.add(w);
                score += 10 + Math.min(occ - 1, 3) * 2;          // repeated mentions add a little
            } else {
                /* [CO3] Wagner-Fischer fuzzy match against words of the description */
                String near = FuzzySearch.nearestWord(blob, w);
                if (near != null) {
                    h.fuzzyHits++;
                    h.fuzzyWords.add(w + "~" + near);
                    score += 4;
                }
            }
        }

        /* [CO2] Rabin-Karp rolling hash for brand / type presence in the query */
        String q = rawQuery.toLowerCase();
        if (PatternSearch.rabinKarp(q, pr.brand.toLowerCase())) { h.brandMatch = true; score += 12; }
        if (PatternSearch.rabinKarp(q, pr.type.toLowerCase())) { h.typeMatch = true; score += 14; }

        /* If the TYPE itself was misspelled ("runing shoos"), Rabin-Karp above finds
         * nothing. Fall back to Damerau-Levenshtein between query keywords and the
         * words of the product type, so "shoos" still points at "Shoes". */
        if (!h.typeMatch) {
            StrList tw = Text.tokenize(pr.type.toLowerCase());
            int hitWords = 0;
            for (int i = 0; i < tw.size(); i++) {
                String t = tw.get(i);
                if (t.length() < 4) continue;
                for (int j = 0; j < kw.size(); j++) {
                    int allow = t.length() >= 7 ? 2 : 1;
                    if (FuzzySearch.damerau(t, kw.get(j)) <= allow) { hitWords++; break; }
                }
            }
            if (hitWords > 0) {
                h.typeMatch = true;
                score += 10 * hitWords;          // more type words matched = stronger signal
            }
        }

        /* [CO2] Z-function prefix style matching on product name */
        for (int i = 0; i < kw.size() && !h.nameMatch; i++)
            if (PatternSearch.zMatch(pr.nameLower, kw.get(i))) { h.nameMatch = true; score += 6; }

        /* category match */
        if (category != null && !category.equals("All Products") && pr.category.equals(category)) {
            h.catMatch = true; score += 5;
        }
        if (PatternSearch.rabinKarp(q, pr.category.toLowerCase())) { h.catMatch = true; score += 8; }

        /* budget scoring */
        if (budget > 0) {
            if (pr.price <= budget) {
                h.budgetOk = true;
                int slack = budget - pr.price;
                score += 10 - Math.min(9, slack * 10 / Math.max(1, budget));   // closer to budget = better value
            } else {
                score -= 15;                                                    // over budget penalty
            }
        }
        h.score = score;
        return h;
    }

        /* [CO2][CO3] ---------- product-type intent ----------
     * Works out WHICH kind of product the requirement asks for ("smartphone", "phones",
     * "runing shoes", "earphones", "wireless headphones ..."): the catalog type whose name
     * best matches the keywords, allowing plurals, synonyms, joined/split words and small
     * typos. The search is then limited to that type, so a request for a smartphone never
     * returns monitors. No type named -> empty list -> normal keyword scoring for everything. */
    StrList typeNames;
    int typeMode = 1;   // 0 = ignore product types, 1 = only the best matching type, 2 = every type named (bundles)

    StrList wantedTypes(StrList kw) {
        if (typeMode == 0) return new StrList();
        if (typeNames == null) {
            typeNames = new StrList();
            for (int i = 0; i < cat.count; i++) {
                String t = cat.products[i].type.toLowerCase();
                if (!typeNames.contains(t)) typeNames.add(t);
            }
        }
        StrList words = new StrList();
        StringBuilder jq = new StringBuilder();
        for (int i = 0; i < kw.size(); i++) {
            String w = kw.get(i);
            words.add(w); jq.append(w);
            String s = synonym(w);
            if (s != null) words.add(s);
        }
        String joinedQ = jq.toString();
        int[] sc = new int[typeNames.size()];
        int best = 0;
        for (int i = 0; i < typeNames.size(); i++) {
            String tl = typeNames.get(i), joined = tl.replace(" ", "");
            int s = 0;
            if (joined.length() >= 6 && PatternSearch.kmpSearch(joinedQ, joined) >= 0) s = 100 + joined.length();
            else {
                StrList tw = Text.tokenize(tl);
                int hit = 0, len = 0;
                for (int a = 0; a < tw.size(); a++)
                    for (int b = 0; b < words.size(); b++)
                        if (sameWord(tw.get(a), words.get(b))) { hit++; len += tw.get(a).length(); break; }
                if (hit == tw.size()) s = 100 + len;      // every word of the type is in the query
                else if (hit > 0) s = len;                // only some words (e.g. "watch" for "Smart Watch")
            }
            sc[i] = s;
            if (s > best) best = s;
        }
        StrList out = new StrList();
        if (best == 0) return out;
        for (int i = 0; i < sc.length; i++)
            if (sc[i] == best || (typeMode == 2 && best >= 100 && sc[i] >= 100)) out.add(typeNames.get(i));
        return out;
    }

    /* type word t vs query word k: equal, simple plural, or within a typo (Damerau) */
    boolean sameWord(String t, String k) {
        if (t.equals(k)) return true;
        if (k.length() > t.length() && k.length() <= t.length() + 2 && k.startsWith(t)) return true;
        if (t.length() >= 4 && k.length() >= 4 && Math.abs(t.length() - k.length()) <= 2)
            return FuzzySearch.damerau(t, k) <= (t.length() >= 7 ? 2 : 1);
        return false;
    }

    static String synonym(String w) {
        String[][] s = {{"phone", "smartphone"}, {"phones", "smartphone"}, {"mobile", "smartphone"}, {"mobiles", "smartphone"},
                {"cellphone", "smartphone"}, {"earphone", "earbuds"}, {"earphones", "earbuds"}, {"headset", "headphones"},
                {"fridge", "refrigerator"}, {"television", "tv"}, {"sofas", "sofa"}, {"couch", "sofa"}};
        for (int i = 0; i < s.length; i++) if (s[i][0].equals(w)) return s[i][1];
        return null;
    }

    static boolean hasAny(StrList toks, String... words) {
        for (int i = 0; i < toks.size(); i++) for (String w : words) if (toks.get(i).equals(w)) return true;
        return false;
    }

    /* ---------- YES / PARTIALLY / NO ---------- */
    String verdict(Hit h, int totalKeywords) {
        if (totalKeywords == 0) totalKeywords = 1;
        double cover = (double) h.keywordHits / totalKeywords;
        boolean budgetFail = (h.budget > 0 && !h.budgetOk);
        if (budgetFail) return "NO";
        if (h.typeIntent && (cover >= 0.5 || totalKeywords <= 1)) return "YES";
        if (cover >= 0.6 && (h.typeMatch || h.catMatch || h.keywordHits >= 2)) return "YES";
        if (cover >= 0.6) return "YES";
        if (h.keywordHits + h.fuzzyHits > 0 || h.lcsLen >= 8) return "PARTIALLY";
        return "NO";
    }

    /* ---------- the WHY THIS PRODUCT explanation ---------- */
    String explain(Hit h, StrList kw) {
        Product p = pr(h);
        StringBuilder sb = new StringBuilder();
        sb.append("\n================ WHY THIS PRODUCT? ================\n");
        sb.append("Suitability : ").append(h.verdict).append("   (relevance score ").append(h.score).append(")\n");
        sb.append("---------------------------------------------------\n");

        sb.append("1) MATCHING KEYWORDS / FEATURES\n   ");
        if (h.matched.size() == 0) sb.append("no exact keyword from your requirement was found.\n");
        else {
            for (int i = 0; i < h.matched.size(); i++) sb.append(h.matched.get(i)).append(i < h.matched.size() - 1 ? ", " : "");
            sb.append("\n   (found in the product description)\n");
        }
        if (h.fuzzyWords.size() > 0) {
            sb.append("   spelling-corrected matches: ");
            for (int i = 0; i < h.fuzzyWords.size(); i++) sb.append(h.fuzzyWords.get(i)).append(" ");
            sb.append("\n   (matched despite spelling differences)\n");
        }
        sb.append("   covered ").append(h.keywordHits).append(" of ").append(kw.size()).append(" requirement keywords\n");

        sb.append("2) DESCRIPTION MATCH\n");
        sb.append("   longest common text segment with your requirement : ").append(h.lcsLen)
          .append(" characters\n");
        sb.append("   local alignment score : ").append(h.alignScore).append("\n");

        sb.append("3) CATEGORY / TYPE MATCH\n");
        sb.append("   category : ").append(p.category).append(h.catMatch ? "  [matches your selection/query]" : "  [not asked for explicitly]").append("\n");
        sb.append("   type     : ").append(p.type).append(h.typeMatch ? "  [you asked for this type]" : "").append("\n");
        if (h.brandMatch) sb.append("   brand    : ").append(p.brand).append("  [you named this brand]\n");

        sb.append("4) BUDGET / PRICE MATCH\n");
        if (h.budget > 0) {
            if (h.budgetOk) sb.append("   price Rs ").append(p.price).append(" is within your budget of Rs ")
                               .append(h.budget).append(" (you save Rs ").append(h.budget - p.price).append(")\n");
            else sb.append("   price Rs ").append(p.price).append(" EXCEEDS your budget of Rs ").append(h.budget)
                    .append(" by Rs ").append(p.price - h.budget).append("\n");
        } else sb.append("   price Rs ").append(p.price).append("  (no budget was specified in your requirement)\n");

        sb.append("5) REQUIREMENT MATCH - FINAL DECISION\n   ");
        if (h.verdict.equals("YES"))
            sb.append("YES - this product satisfies most of what you asked for, and the price fits.\n");
        else if (h.verdict.equals("PARTIALLY"))
            sb.append("PARTIALLY - it matches some of your requirement but misses ")
              .append(kw.size() - h.keywordHits).append(" keyword(s). Check the details before buying.\n");
        else
            sb.append("NO - this product does not really satisfy your requirement")
              .append(h.budget > 0 && !h.budgetOk ? " because it is over your budget.\n" : ".\n");
        sb.append("===================================================\n");
        return sb.toString();
    }

    /* ---------- diversity filter : Vertex Cover 2-approximation (CO5) ----------
     * Two hits are "near duplicates" if same brand+type. We build that graph,
     * take a 2-approximate vertex cover, and drop the covered nodes that are
     * lower ranked, so the list shown is not 10 versions of the same thing. */
    Hit[] diversify(Hit[] hits, int keep) {
        int n = Math.min(hits.length, 16);
        if (n <= 2) return hits;
        boolean[][] g = new boolean[n][n];
        int edges = 0;
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++) {
                Product a = pr(hits[i]), b = pr(hits[j]);
                if (a.brand.equals(b.brand) && a.type.equals(b.type)) { g[i][j] = true; g[j][i] = true; edges++; }
            }
        if (edges == 0) return hits;
        boolean[] cover = GreedyRecommendation.vertexCover2Approx(g);
        Hit[] out = new Hit[keep];
        int k = 0;
        for (int i = 0; i < n && k < keep; i++) if (!cover[i]) out[k++] = hits[i];
        for (int i = 0; i < n && k < keep; i++) if (cover[i]) out[k++] = hits[i];
        for (int i = n; i < hits.length && k < keep; i++) out[k++] = hits[i];
        Hit[] real = new Hit[k];
        for (int i = 0; i < k; i++) real[i] = out[i];
        return real;
    }
}
