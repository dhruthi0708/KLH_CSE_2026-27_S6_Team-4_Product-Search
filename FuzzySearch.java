/*
 * FuzzySearch.java -- CO3 (typo tolerance)
 * Wagner-Fischer edit distance, Damerau-Levenshtein, nearest-word lookup.
 */
public class FuzzySearch {

/* ---------- Wagner-Fischer : Levenshtein edit distance ---------- */
    /* [CO3] TOPIC: Wagner-Fischer DP, Levenshtein distance (insert, delete, substitute).
     * IN PROJECT: reference version of the edit-distance table. */
    public static int editDistance(String a, String b) {
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) dp[i][0] = i;
        for (int j = 0; j <= m; j++) dp[0][j] = j;
        for (int i = 1; i <= n; i++)
            for (int j = 1; j <= m; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                int best = dp[i - 1][j] + 1;                       // deletion
                if (dp[i][j - 1] + 1 < best) best = dp[i][j - 1] + 1;   // insertion
                if (dp[i - 1][j - 1] + cost < best) best = dp[i - 1][j - 1] + cost; // substitution
                dp[i][j] = best;
            }
        return dp[n][m];
    }

    /* ---------- Damerau-Levenshtein : also allows transposition (teh -> the) ---------- */
    /* [CO3] TOPIC: Damerau-Levenshtein, edit distance that also counts a swap of two neighbours as 1.
     * IN PROJECT: typo tolerance when scoring products and when suggesting corrections. */
    public static int damerau(String a, String b) {
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) dp[i][0] = i;
        for (int j = 0; j <= m; j++) dp[0][j] = j;
        for (int i = 1; i <= n; i++)
            for (int j = 1; j <= m; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                int best = dp[i - 1][j] + 1;
                if (dp[i][j - 1] + 1 < best) best = dp[i][j - 1] + 1;
                if (dp[i - 1][j - 1] + cost < best) best = dp[i - 1][j - 1] + cost;
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1))
                    if (dp[i - 2][j - 2] + 1 < best) best = dp[i - 2][j - 2] + 1;
                dp[i][j] = best;
            }
        return dp[n][m];
    }

    /* nearest description word within edit distance 1 or 2 (typo tolerance) */
    /* [CO3] TOPIC: closest word within 1-2 edits.
     * IN PROJECT: SearchEngine credits a product whose description holds a misspelled keyword. */
    static String nearestWord(String blob, String w) {
        if (w.length() < 4) return null;
        StrList words = Text.tokenize(blob);
        int allow = w.length() >= 7 ? 2 : 1;
        String best = null; int bd = 99;
        for (int i = 0; i < words.size(); i++) {
            String v = words.get(i);
            if (Math.abs(v.length() - w.length()) > allow) continue;
            int d = FuzzySearch.damerau(w, v);      // Damerau-Levenshtein handles swapped letters
            if (d <= allow && d < bd) { bd = d; best = v; if (d == 1) break; }
        }
        return best;
    }

    /*
     * "Did you mean ...?"  (fuzzy search / spelling correction)
     * For every keyword the catalog vocabulary has never seen, find the closest real
     * word in the inverted index by Damerau-Levenshtein distance (max 1 edit, 2 for
     * long words). Ties go to the word used by more products.
     * Returns the corrected query, or null when nothing needed fixing.
     */
    /* [CO3] TOPIC: spelling correction against the catalog vocabulary.
     * IN PROJECT: drives the "Did you mean" prompt before a search. */
    static String suggest(String query, ProductRepository repo) {
        StrList toks = Text.tokenize(query);
        StringBuilder out = new StringBuilder();
        boolean changed = false;
        for (int i = 0; i < toks.size(); i++) {
            String w = toks.get(i), fix = w;
            boolean num = w.charAt(0) >= '0' && w.charAt(0) <= '9';
            if (w.length() >= 4 && !num && !Text.isStop(w) && repo.index.get(w) == null) {
                int allow = w.length() >= 7 ? 2 : 1, bd = 99, bf = -1;
                for (int j = 0; j < repo.index.M; j++) {
                    String v = repo.index.keys[j];
                    if (v == null || Math.abs(v.length() - w.length()) > allow) continue;
                    int d = damerau(w, v), f = repo.index.vals[j].size();
                    if (d <= allow && (d < bd || (d == bd && f > bf))) { bd = d; bf = f; fix = v; }
                }
            }
            if (!fix.equals(w)) changed = true;
            out.append(fix).append(i < toks.size() - 1 ? " " : "");
        }
        return changed ? out.toString() : null;
    }
}
