/*
 * ProblemClassifier.java -- CO1 : evaluate problem-class signatures and pick an
 * advanced-algorithm strategy.
 * Reads the user's query, detects which problem classes it contains, names the
 * algorithm family for each, then MEASURES naive vs KMP on the real catalog to
 * justify the choice. Nothing is hardcoded per query.
 */
public class ProblemClassifier {

    static void row(StringBuilder sb, String problem, String family, String algo, String why) {
        sb.append(" * ").append(problem).append("\n")
          .append("     kind      : ").append(family).append("\n")
          .append("     approach  : ").append(algo).append("\n")
          .append("     because   : ").append(why).append("\n");
    }

    static String report(String query, ProductRepository repo) {
        StrList kw = Text.keywords(query);
        int budget = Text.extractBudget(query);

        /* CO1 mapping (names live here, not in the output):
         *   keyword search -> KMP | spelling -> Damerau-Levenshtein | similarity -> suffix array + Kasai LCP
         *   bundle -> 0/1 knapsack | assignment -> Edmonds-Karp matching | ranking -> randomised quicksort, vertex cover 2-approx
         */
        /* signature 1: keywords the inverted index has never seen => probable typos */
        StrList typo = new StrList();
        for (int i = 0; i < kw.size(); i++) {
            String w = kw.get(i);
            boolean num = w.charAt(0) >= '0' && w.charAt(0) <= '9';
            if (w.length() >= 4 && !num && repo.index.get(w) == null) typo.add(w);
        }
        long n = 0;                                           // total characters to be searched
        for (int i = 0; i < repo.count; i++) n += repo.products[i].descLower.length();

        StringBuilder sb = new StringBuilder();
        sb.append("\n=========== CO1 : PROBLEM CLASSIFICATION ===========\n");
        sb.append("query has ").append(kw.size()).append(" keyword(s), ")
          .append(typo.size()).append(" unknown word(s), budget ").append(budget > 0 ? "Rs " + budget : "none")
          .append("; catalog text n = ").append(n).append(" chars\n");

        if (kw.size() > 0)
            row(sb, "Keyword search", "text matching", "linear-time matcher  O(n+m)  (a plain scan is O(n*m))",
                "every keyword must be located inside " + repo.count + " descriptions");
        if (typo.size() > 0)
            row(sb, "Spelling tolerance", "edit-distance table", "cost O(a*b) per word pair",
                "unknown word(s) " + list(typo) + " look like misspellings");
        if (query.length() >= 25)
            row(sb, "Text similarity", "suffix-based comparison", "cost O(n log^2 n)",
                "long free-text requirement compared with whole descriptions");
        if (budget > 0)
            row(sb, "Budget-constrained bundle", "optimisation", "table-based selection  O(n*W)",
                "choosing a best SET of products under Rs " + budget + " is a hard optimisation problem");
        if (kw.size() >= 3)
            row(sb, "Aspect assignment", "graph flow", "flow-based assignment  O(V*E^2)",
                kw.size() + " aspects each need a distinct product = maximum matching");
        row(sb, "Ranking + diversity", "randomised + approximation",
            "randomised sort E[O(n log n)] + near-duplicate removal",
            "score sorting must avoid worst-case pivots; near-duplicate removal is NP-hard");

        /* evidence: run both matchers on the real catalog */
        if (kw.size() > 0) {
            long sn = PatternSearch.naiveComparisons, sk = PatternSearch.kmpComparisons;
            PatternSearch.naiveComparisons = 0; PatternSearch.kmpComparisons = 0;
            long t0 = System.nanoTime();
            for (int i = 0; i < repo.count; i++)
                for (int j = 0; j < kw.size(); j++) PatternSearch.naiveSearch(repo.products[i].descLower, kw.get(j));
            long tn = (System.nanoTime() - t0) / 1000;
            t0 = System.nanoTime();
            for (int i = 0; i < repo.count; i++)
                for (int j = 0; j < kw.size(); j++) PatternSearch.kmpSearch(repo.products[i].descLower, kw.get(j));
            long tk = (System.nanoTime() - t0) / 1000;
            long cn = PatternSearch.naiveComparisons, ck = PatternSearch.kmpComparisons;
            PatternSearch.naiveComparisons = sn; PatternSearch.kmpComparisons = sk;

            sb.append("---------------------------------------------------\n");
            sb.append("MEASURED on this query:  plain scan ").append(cn).append(" comparisons (").append(tn).append(" us)")
              .append("  |  optimised matcher ").append(ck).append(" comparisons (").append(tk).append(" us)\n");
            sb.append("SELECTED: optimised matcher - ").append(ck <= cn ? "fewer comparisons here and " : "the plain scan is cheaper on this data, but only the optimised matcher ")
              .append("guarantees O(n+m) on any text.\n");
        }
        sb.append("===================================================\n");
        return sb.toString();
    }

    static String list(StrList s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.size(); i++) b.append(s.get(i)).append(i < s.size() - 1 ? ", " : "");
        return b.toString();
    }
}
