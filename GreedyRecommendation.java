/*
 * GreedyRecommendation.java -- CO3 + CO5 (recommendation / optimisation)
 * 0/1 Knapsack bundle, Optimal BST, Bitmask-DP TSP route,
 * Vertex Cover 2-approximation (diversity) and exact vertex cover baseline.
 */
public class GreedyRecommendation {

/* ---------- Interval DP : Optimal Binary Search Tree cost ----------
     * Used to build a search-cost-optimal ordering of frequently viewed products. */
    /* [CO3] TOPIC: interval DP, optimal binary search tree cost.
     * IN PROJECT: cost model for arranging frequently viewed products. */
    public static int optimalBSTCost(int[] freq) {
        int n = freq.length;
        if (n == 0) return 0;
        int[][] dp = new int[n][n];
        int[] pre = new int[n + 1];
        for (int i = 0; i < n; i++) pre[i + 1] = pre[i] + freq[i];
        for (int i = 0; i < n; i++) dp[i][i] = freq[i];
        for (int len = 2; len <= n; len++)
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                int sum = pre[j + 1] - pre[i];
                int best = Integer.MAX_VALUE;
                for (int r = i; r <= j; r++) {
                    int left = (r > i) ? dp[i][r - 1] : 0;
                    int right = (r < j) ? dp[r + 1][j] : 0;
                    if (left + right < best) best = left + right;
                }
                dp[i][j] = best + sum;
            }
        return dp[0][n - 1];
    }

    /* ---------- Bitmask DP : Travelling Salesperson ----------
     * Used to order cart items into the shortest shop-section pickup route. */
    /* [CO3] TOPIC: bitmask DP for the travelling salesperson problem, O(2^n * n^2).
     * IN PROJECT: orders cart items into the shortest store pickup route. */
    public static int[] tspOrder(int[][] dist) {
        int n = dist.length;
        if (n == 0) return new int[0];
        if (n == 1) return new int[]{0};
        int full = 1 << n;
        int[][] dp = new int[full][n];
        int[][] par = new int[full][n];
        for (int m = 0; m < full; m++)
            for (int i = 0; i < n; i++) { dp[m][i] = Integer.MAX_VALUE / 4; par[m][i] = -1; }
        dp[1][0] = 0;
        for (int mask = 1; mask < full; mask++)
            for (int last = 0; last < n; last++) {
                if ((mask & (1 << last)) == 0 || dp[mask][last] >= Integer.MAX_VALUE / 4) continue;
                for (int nxt = 0; nxt < n; nxt++) {
                    if ((mask & (1 << nxt)) != 0) continue;
                    int nm = mask | (1 << nxt);
                    int cost = dp[mask][last] + dist[last][nxt];
                    if (cost < dp[nm][nxt]) { dp[nm][nxt] = cost; par[nm][nxt] = last; }
                }
            }
        int end = 0, best = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) if (dp[full - 1][i] < best) { best = dp[full - 1][i]; end = i; }
        int[] order = new int[n];
        int mask = full - 1, cur = end;
        for (int k = n - 1; k >= 0; k--) { order[k] = cur; int p = par[mask][cur]; mask ^= (1 << cur); cur = p; if (cur < 0) break; }
        return order;
    }

    /* ---------- 0/1 Knapsack DP : best value bundle inside a budget ----------
     * value[] = relevance score of product, weight[] = price. */
    /* [CO3/CO5] TOPIC: 0/1 knapsack DP (value = relevance, weight = price).
     * IN PROJECT: picks the best bundle of products inside the budget. */
    public static IntList knapsackPick(int[] value, int[] weight, int budget) {
        int n = value.length;
        int[][] dp = new int[n + 1][budget + 1];
        for (int i = 1; i <= n; i++)
            for (int w = 0; w <= budget; w++) {
                dp[i][w] = dp[i - 1][w];
                if (weight[i - 1] <= w && dp[i - 1][w - weight[i - 1]] + value[i - 1] > dp[i][w])
                    dp[i][w] = dp[i - 1][w - weight[i - 1]] + value[i - 1];
            }
        IntList picked = new IntList();
        int w = budget;
        for (int i = n; i >= 1; i--)
            if (dp[i][w] != dp[i - 1][w]) { picked.add(i - 1); w -= weight[i - 1]; }
        return picked;
    }

/* ================= CO5 : NP-HARD + APPROXIMATION ================= */

    /*
     * Vertex Cover 2-approximation using maximal matching.
     * Graph edge (i,j) = "product i and product j are near-duplicates".
     * The cover is the redundant set we DROP so the shown list stays diverse.
     */
    /* [CO5] TOPIC: vertex cover 2-approximation through a maximal matching (NP-hard problem, ratio <= 2).
     * IN PROJECT: marks near-duplicate results so the shown list stays varied. */
    public static boolean[] vertexCover2Approx(boolean[][] g) {
        int n = g.length;
        boolean[] cover = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (cover[i]) continue;
            for (int j = i + 1; j < n; j++) {
                if (g[i][j] && !cover[j]) { cover[i] = true; cover[j] = true; break; }  // pick both ends
            }
        }
        return cover;
    }

    /* Exact brute-force vertex cover (exponential) - used only for tiny n, to show
     * the gap between exact NP-hard solving and the 2-approximation. */
    /* [CO5] TOPIC: exact vertex cover by brute force, exponential.
     * IN PROJECT: tiny-input baseline to compare against the 2-approximation. */
    public static int exactVertexCover(boolean[][] g) {
        int n = g.length;
        if (n > 20) return -1;
        int best = n;
        for (int mask = 0; mask < (1 << n); mask++) {
            int c = Integer.bitCount(mask);
            if (c >= best) continue;
            boolean ok = true;
            for (int i = 0; i < n && ok; i++)
                for (int j = i + 1; j < n; j++)
                    if (g[i][j] && (mask & (1 << i)) == 0 && (mask & (1 << j)) == 0) { ok = false; break; }
            if (ok) best = c;
        }
        return best;
    }
}
