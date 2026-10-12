/* Sorter - manual stable merge sort that returns a sorted permutation (index array). */
class Sorter {
    private long[] lk; private String[] sk; private boolean asc;

    static int[] byLong(long[] key, boolean asc) { Sorter s = new Sorter(); s.lk = key; s.asc = asc; return s.run(key.length); }
    static int[] byString(String[] key, boolean asc) { Sorter s = new Sorter(); s.sk = key; s.asc = asc; return s.run(key.length); }

    private int[] run(int n) {
        int[] p = new int[n], tmp = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
        sort(p, tmp, 0, n);
        return p;
    }
    /* true when element a may stay before element b (keeps the sort stable) */
    private boolean before(int a, int b) {
        int c = lk != null ? (lk[a] < lk[b] ? -1 : lk[a] > lk[b] ? 1 : 0) : cmp(sk[a], sk[b]);
        return asc ? c <= 0 : c >= 0;
    }
    private void sort(int[] p, int[] t, int lo, int hi) {
        if (hi - lo < 2) return;
        int mid = (lo + hi) >>> 1;
        sort(p, t, lo, mid); sort(p, t, mid, hi);
        int i = lo, j = mid, k = lo;
        while (i < mid && j < hi) t[k++] = before(p[i], p[j]) ? p[i++] : p[j++];
        while (i < mid) t[k++] = p[i++];
        while (j < hi) t[k++] = p[j++];
        for (k = lo; k < hi; k++) p[k] = t[k];
    }
    static int cmp(String a, String b) {
        int n = Math.min(a.length(), b.length());
        for (int i = 0; i < n; i++) {
            char x = Character.toLowerCase(a.charAt(i)), y = Character.toLowerCase(b.charAt(i));
            if (x != y) return x < y ? -1 : 1;
        }
        return a.length() - b.length();
    }
}
