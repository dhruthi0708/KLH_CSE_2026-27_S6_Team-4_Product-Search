/*
 * ParallelSearch.java -- CO6 (randomised + parallel)
 * Randomised quicksort, Miller-Rabin, reservoir sampling, parallel prefix-sum / reduce.
 */
public class ParallelSearch {

/* ================= CO6 : RANDOMISED ================= */

    /* Miller-Rabin probabilistic primality test - used for order-ID validation. */
    /* [CO6] TOPIC: Miller-Rabin, randomised (Monte Carlo) primality test.
     * IN PROJECT: checks that the order ID generated at checkout is prime. */
    public static boolean millerRabin(long n, int rounds, Rand r) {
        if (n < 2) return false;
        for (long p : new long[]{2, 3, 5, 7, 11, 13, 17, 19, 23}) {
            if (n == p) return true;
            if (n % p == 0) return false;
        }
        long d = n - 1; int s = 0;
        while (d % 2 == 0) { d /= 2; s++; }
        for (int it = 0; it < rounds; it++) {
            long a = 2 + r.next((int) Math.min(n - 4, 1000000));   // random witness
            long x = powMod(a, d, n);
            if (x == 1 || x == n - 1) continue;
            boolean composite = true;
            for (int i = 1; i < s; i++) {
                x = mulMod(x, x, n);
                if (x == n - 1) { composite = false; break; }
            }
            if (composite) return false;
        }
        return true;    // Monte Carlo: "probably prime"
    }

    static long mulMod(long a, long b, long m) {
        long res = 0; a %= m;
        while (b > 0) { if ((b & 1) == 1) res = (res + a) % m; a = (a * 2) % m; b >>= 1; }
        return res;
    }

    static long powMod(long b, long e, long m) {
        long res = 1; b %= m;
        while (e > 0) { if ((e & 1) == 1) res = mulMod(res, b, m); b = mulMod(b, b, m); e >>= 1; }
        return res;
    }

    /*
     * Randomised QuickSort (Las Vegas) on scores, descending.
     * Random pivot => expected O(n log n), avoids worst case on sorted input.
     */
    /* [CO6] TOPIC: randomised quicksort (Las Vegas), random pivot, expected O(n log n).
     * IN PROJECT: ranks search results by score. */
    public static void randomizedQuickSort(int[] score, int[] idx, int lo, int hi, Rand r) {
        if (lo >= hi) return;
        int p = lo + r.next(hi - lo + 1);
        swap(score, idx, p, hi);
        int pivot = score[hi], i = lo - 1;
        for (int j = lo; j < hi; j++) if (score[j] > pivot) { i++; swap(score, idx, i, j); }
        swap(score, idx, i + 1, hi);
        randomizedQuickSort(score, idx, lo, i, r);
        randomizedQuickSort(score, idx, i + 2, hi, r);
    }

    static void swap(int[] a, int[] b, int i, int j) {
        int t = a[i]; a[i] = a[j]; a[j] = t;
        int u = b[i]; b[i] = b[j]; b[j] = u;
    }

    /* Reservoir sampling : pick k uniformly at random from a stream of unknown size.
     * Used for "Discover / surprise me" picks so results are never hardcoded. */
    /* [CO6] TOPIC: reservoir sampling, k uniform picks from a list of unknown length.
     * IN PROJECT: "Surprise picks" in a category. */
    public static int[] reservoirSample(IntList stream, int k, Rand r) {
        int n = stream.size();
        if (n <= k) { int[] out = new int[n]; for (int i = 0; i < n; i++) out[i] = stream.get(i); return out; }
        int[] res = new int[k];
        for (int i = 0; i < k; i++) res[i] = stream.get(i);
        for (int i = k; i < n; i++) {
            int j = r.next(i + 1);
            if (j < k) res[j] = stream.get(i);
        }
        return res;
    }

    /* ================= CO6 : PARALLEL ================= */

    /* Sequential prefix sum : work T1 = O(n), span = O(n). */
    /* [CO6] TOPIC: ordinary prefix sum, O(n).
     * IN PROJECT: reference result the parallel scan is checked against. */
    public static long[] sequentialPrefixSum(long[] a) {
        long[] p = new long[a.length];
        long s = 0;
        for (int i = 0; i < a.length; i++) { s += a[i]; p[i] = s; }
        return p;
    }

    /* Hillis-Steele parallel scan: log n rounds, each round split across threads.
     * Work T1 = O(n log n), span T_inf = O(log n). Used for running cart totals. */
    /* [CO6] TOPIC: parallel prefix-sum (scan) over threads.
     * IN PROJECT: running totals of the cart. */
    public static long[] parallelPrefixSum(long[] a) throws Exception {
        int n = a.length;
        long[] out = new long[n];
        for (int i = 0; i < n; i++) out[i] = a[i];
        int threads = Runtime.getRuntime().availableProcessors();
        for (int d = 1; d < n; d *= 2) {
            final long[] src = new long[n];
            for (int i = 0; i < n; i++) src[i] = out[i];
            final int dd = d;
            Thread[] ts = new Thread[threads];
            int chunk = (n + threads - 1) / threads;
            for (int t = 0; t < threads; t++) {
                final int lo = t * chunk, hi = Math.min(n, lo + chunk);
                final long[] dst = out;
                ts[t] = new Thread(() -> {
                    for (int i = lo; i < hi; i++) dst[i] = (i >= dd) ? src[i] + src[i - dd] : src[i];
                });
                ts[t].start();
            }
            for (Thread th : ts) th.join();
        }
        return out;
    }

    /* Parallel reduce : sum of an array split across threads. */
    /* [CO6] TOPIC: parallel reduce, threads sum chunks then combine.
     * IN PROJECT: cart grand total. */
    public static long parallelReduce(long[] a) throws Exception {
        int threads = Runtime.getRuntime().availableProcessors();
        int n = a.length;
        long[] partial = new long[threads];
        Thread[] ts = new Thread[threads];
        int chunk = (n + threads - 1) / threads;
        for (int t = 0; t < threads; t++) {
            final int id = t, lo = t * chunk, hi = Math.min(n, lo + chunk);
            ts[t] = new Thread(() -> { long s = 0; for (int i = lo; i < hi; i++) s += a[i]; partial[id] = s; });
            ts[t].start();
        }
        for (Thread th : ts) th.join();
        long total = 0;
        for (long v : partial) total += v;
        return total;
    }
}
