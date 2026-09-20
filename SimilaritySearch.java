/*
 * SimilaritySearch.java -- CO2 + CO3 (document similarity)
 * Suffix array, Kasai LCP, longest common substring, Needleman-Wunsch, Smith-Waterman.
 */
public class SimilaritySearch {

/* ---------- Suffix Array : O(n log^2 n) doubling ---------- */
    /* [CO2] TOPIC: suffix array, all suffixes in sorted order (prefix doubling, O(n log^2 n)).
     * IN PROJECT: foundation of the description-similarity measure. */
    public static int[] suffixArray(String s) {
        int n = s.length();
        int[] sa = new int[n], rank = new int[n], tmp = new int[n];
        for (int i = 0; i < n; i++) { sa[i] = i; rank[i] = s.charAt(i); }
        for (int k = 1; k < n; k *= 2) {
            final int kk = k; final int[] rk = rank;
            sortSA(sa, rk, kk, n);
            tmp[sa[0]] = 0;
            for (int i = 1; i < n; i++)
                tmp[sa[i]] = tmp[sa[i - 1]] + (cmp(sa[i - 1], sa[i], rk, kk, n) < 0 ? 1 : 0);
            for (int i = 0; i < n; i++) rank[i] = tmp[i];
            if (rank[sa[n - 1]] == n - 1) break;
        }
        return sa;
    }

    static int cmp(int x, int y, int[] rank, int k, int n) {
        if (rank[x] != rank[y]) return rank[x] - rank[y];
        int rx = (x + k < n) ? rank[x + k] : -1;
        int ry = (y + k < n) ? rank[y + k] : -1;
        return rx - ry;
    }

    /* simple insertion-free merge sort over sa (manual, no java.util.Arrays.sort with comparator) */
    static void sortSA(int[] sa, int[] rank, int k, int n) {
        int[] buf = new int[n];
        msort(sa, buf, 0, n - 1, rank, k, n);
    }

    static void msort(int[] a, int[] buf, int lo, int hi, int[] rank, int k, int n) {
        if (lo >= hi) return;
        int mid = (lo + hi) / 2;
        msort(a, buf, lo, mid, rank, k, n);
        msort(a, buf, mid + 1, hi, rank, k, n);
        int i = lo, j = mid + 1, t = lo;
        while (i <= mid && j <= hi) buf[t++] = (cmp(a[i], a[j], rank, k, n) <= 0) ? a[i++] : a[j++];
        while (i <= mid) buf[t++] = a[i++];
        while (j <= hi) buf[t++] = a[j++];
        for (int x = lo; x <= hi; x++) a[x] = buf[x];
    }

    /* ---------- Kasai's algorithm : LCP array in O(n) ---------- */
    /* [CO2] TOPIC: Kasai algorithm, LCP array (common prefix of neighbouring suffixes) in O(n).
     * IN PROJECT: read off the longest text shared by two documents. */
    public static int[] kasaiLCP(String s, int[] sa) {
        int n = s.length();
        int[] rank = new int[n], lcp = new int[n];
        for (int i = 0; i < n; i++) rank[sa[i]] = i;
        int h = 0;
        for (int i = 0; i < n; i++) {
            if (rank[i] > 0) {
                int j = sa[rank[i] - 1];
                while (i + h < n && j + h < n && s.charAt(i + h) == s.charAt(j + h)) h++;
                lcp[rank[i]] = h;
                if (h > 0) h--;
            } else h = 0;
        }
        return lcp;
    }

    /*
     * Longest common substring length between query and description,
     * via suffix array + LCP on (query + '#' + description).
     * This is the document-similarity measure used in recommendations.
     */
    /* [CO2] TOPIC: longest common substring through suffix array + LCP.
     * IN PROJECT: similarity score added to top search hits, and used by "similar products". */
    public static int longestCommonSubstring(String a, String b) {
        if (a.length() == 0 || b.length() == 0) return 0;
        String s = a + "\u0001" + b + "\u0002";
        int[] sa = suffixArray(s);
        int[] lcp = kasaiLCP(s, sa);
        int split = a.length();
        int best = 0;
        for (int i = 1; i < sa.length; i++) {
            boolean p = sa[i] < split, q = sa[i - 1] < split;
            if (p != q && lcp[i] > best) best = lcp[i];        // suffixes from different documents
        }
        return best;
    }

/* ---------- Needleman-Wunsch : global alignment score ---------- */
    /* [CO3] TOPIC: Needleman-Wunsch, global alignment score of two whole strings.
     * IN PROJECT: available for whole-string comparison; not on the main search path. */
    public static int needlemanWunsch(String a, String b, int match, int mismatch, int gap) {
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) dp[i][0] = i * gap;
        for (int j = 0; j <= m; j++) dp[0][j] = j * gap;
        for (int i = 1; i <= n; i++)
            for (int j = 1; j <= m; j++) {
                int s = dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? match : mismatch);
                int d = dp[i - 1][j] + gap;
                int ins = dp[i][j - 1] + gap;
                dp[i][j] = Math.max(s, Math.max(d, ins));
            }
        return dp[n][m];
    }

    /* ---------- Smith-Waterman : best LOCAL alignment score ---------- */
    /* [CO3] TOPIC: Smith-Waterman, best LOCAL alignment score (0 restarts the alignment).
     * IN PROJECT: rewards descriptions that contain a close stretch of the requirement text. */
    public static int smithWaterman(String a, String b, int match, int mismatch, int gap) {
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        int best = 0;
        for (int i = 1; i <= n; i++)
            for (int j = 1; j <= m; j++) {
                int s = dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? match : mismatch);
                int d = dp[i - 1][j] + gap;
                int ins = dp[i][j - 1] + gap;
                int v = Math.max(0, Math.max(s, Math.max(d, ins)));   // 0 = restart local alignment
                dp[i][j] = v;
                if (v > best) best = v;
            }
        return best;
    }
}
