/*
 * PatternSearch.java -- CO2 (exact pattern matching)
 * Naive, KMP, Z-function, Rabin-Karp (double hashing).
 */
public class PatternSearch {

public static long naiveComparisons = 0;
    public static long kmpComparisons = 0;

    /* ---------- Naive pattern matching : O(n*m) ---------- */
    /* [CO2] TOPIC: naive pattern matching, tries every alignment, O(n*m).
     * IN PROJECT: baseline only; ProblemClassifier compares its work against the fast matcher. */
    public static int naiveSearch(String text, String pat) {
        int n = text.length(), m = pat.length();
        if (m == 0 || m > n) return -1;
        for (int i = 0; i + m <= n; i++) {
            int j = 0;
            while (j < m) { naiveComparisons++; if (text.charAt(i + j) != pat.charAt(j)) break; j++; }
            if (j == m) return i;
        }
        return -1;
    }

    /* ---------- KMP failure function (LPS array) ---------- */
    /* [CO2] TOPIC: KMP failure function (longest proper prefix that is also a suffix).
     * IN PROJECT: lets the matcher skip re-reading characters after a mismatch. */
    public static int[] buildLPS(String p) {
        int m = p.length();
        int[] lps = new int[m];
        int len = 0, i = 1;
        while (i < m) {
            if (p.charAt(i) == p.charAt(len)) { len++; lps[i] = len; i++; }
            else if (len != 0) len = lps[len - 1];
            else { lps[i] = 0; i++; }
        }
        return lps;
    }

    /* KMP search : O(n+m). Returns first index or -1. */
    /* [CO2] TOPIC: KMP search, O(n+m), first occurrence.
     * IN PROJECT: checks whether a product description contains a requirement word (aspect matching). */
    public static int kmpSearch(String text, String pat) {
        int n = text.length(), m = pat.length();
        if (m == 0 || m > n) return -1;
        int[] lps = buildLPS(pat);
        int i = 0, j = 0;
        while (i < n) {
            kmpComparisons++;
            if (text.charAt(i) == pat.charAt(j)) { i++; j++; if (j == m) return i - m; }
            else if (j != 0) j = lps[j - 1];
            else i++;
        }
        return -1;
    }

    /* KMP count of all occurrences - used for keyword frequency scoring. */
    /* [CO2] TOPIC: KMP counting all occurrences.
     * IN PROJECT: SearchEngine.scoreProduct uses the count to score how strongly a keyword matches. */
    public static int kmpCount(String text, String pat) {
        int n = text.length(), m = pat.length();
        if (m == 0 || m > n) return 0;
        int[] lps = buildLPS(pat);
        int i = 0, j = 0, c = 0;
        while (i < n) {
            kmpComparisons++;
            if (text.charAt(i) == pat.charAt(j)) {
                i++; j++;
                if (j == m) { c++; j = lps[j - 1]; }
            } else if (j != 0) j = lps[j - 1];
            else i++;
        }
        return c;
    }

    /* ---------- Z-function : all Z values in O(n) ---------- */
    /* [CO2] TOPIC: Z-function, length of the longest prefix match at every position, O(n).
     * IN PROJECT: building block for zMatch. */
    public static int[] zFunction(String s) {
        int n = s.length();
        int[] z = new int[n];
        int l = 0, r = 0;
        for (int i = 1; i < n; i++) {
            if (i < r) z[i] = Math.min(r - i, z[i - l]);
            while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) z[i]++;
            if (i + z[i] > r) { l = i; r = i + z[i]; }
        }
        if (n > 0) z[0] = n;
        return z;
    }

    /* Z based match: pattern + '\1' + text. Used for product NAME prefix matching. */
    /* [CO2] TOPIC: pattern search through the Z-function on pattern + separator + text.
     * IN PROJECT: tells whether a query word occurs in a product NAME (name-match bonus in scoring). */
    public static boolean zMatch(String text, String pat) {
        if (pat.length() == 0 || pat.length() > text.length()) return false;
        String c = pat + "\u0001" + text;
        int[] z = zFunction(c);
        for (int i = pat.length() + 1; i < c.length(); i++) if (z[i] >= pat.length()) return true;
        return false;
    }

    /* ---------- Rabin-Karp with polynomial rolling hash + double hashing ---------- */
    static final long B1 = 131, M1 = 1000000007L;
    static final long B2 = 137, M2 = 998244353L;

    /* [CO2] TOPIC: Rabin-Karp, polynomial rolling hash with two moduli, average O(n+m).
     * IN PROJECT: spots brand, type and category names inside the free-text query. */
    public static boolean rabinKarp(String text, String pat) {
        int n = text.length(), m = pat.length();
        if (m == 0 || m > n) return false;
        long p1 = 0, p2 = 0, t1 = 0, t2 = 0, h1 = 1, h2 = 1;
        for (int i = 0; i < m - 1; i++) { h1 = h1 * B1 % M1; h2 = h2 * B2 % M2; }
        for (int i = 0; i < m; i++) {
            p1 = (p1 * B1 + pat.charAt(i)) % M1;
            p2 = (p2 * B2 + pat.charAt(i)) % M2;
            t1 = (t1 * B1 + text.charAt(i)) % M1;
            t2 = (t2 * B2 + text.charAt(i)) % M2;
        }
        for (int i = 0; i + m <= n; i++) {
            if (p1 == t1 && p2 == t2) return true;         // double hash => collisions almost impossible
            if (i + m < n) {
                t1 = ((t1 - text.charAt(i) * h1 % M1 + M1 * M1) % M1 * B1 + text.charAt(i + m)) % M1;
                t2 = ((t2 - text.charAt(i) * h2 % M2 + M2 * M2) % M2 * B2 + text.charAt(i + m)) % M2;
            }
        }
        return false;
    }
}
