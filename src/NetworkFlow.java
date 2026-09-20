/*
 * NetworkFlow.java -- CO4
 * Edmonds-Karp max-flow, min-cut, bipartite matching via flow.
 */
public class NetworkFlow {

/* ================= CO4 : NETWORK FLOW ================= */

    /* Edmonds-Karp : Ford-Fulkerson with BFS augmenting paths, O(V E^2). */
    /* [CO4] TOPIC: Edmonds-Karp max-flow (BFS augmenting paths, O(V*E^2)); the last BFS gives the min-cut.
     * IN PROJECT: general flow engine, also reports the min-cut side. */
    public static int maxFlow(int[][] cap, int s, int t, int[] minCutSide) {
        int n = cap.length;
        int[][] res = new int[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) res[i][j] = cap[i][j];
        int flow = 0;
        int[] parent = new int[n];
        boolean[] vis = new boolean[n];
        while (true) {
            for (int i = 0; i < n; i++) { parent[i] = -1; vis[i] = false; }
            IntQueue q = new IntQueue(n);
            q.push(s); vis[s] = true;
            while (!q.empty()) {
                int u = q.pop();
                for (int v = 0; v < n; v++)
                    if (!vis[v] && res[u][v] > 0) { vis[v] = true; parent[v] = u; q.push(v); }
            }
            if (!vis[t]) {                      // no augmenting path -> max-flow reached
                if (minCutSide != null) for (int i = 0; i < n; i++) minCutSide[i] = vis[i] ? 1 : 0;
                break;                          // vis[] is exactly the source side of the MIN CUT
            }
            int bottleneck = Integer.MAX_VALUE;
            for (int v = t; v != s; v = parent[v]) bottleneck = Math.min(bottleneck, res[parent[v]][v]);
            for (int v = t; v != s; v = parent[v]) { res[parent[v]][v] -= bottleneck; res[v][parent[v]] += bottleneck; }
            flow += bottleneck;
        }
        return flow;
    }

    /*
     * Bipartite matching via max-flow.
     * left = user requirement aspects, right = candidate products.
     * adj[i][j] = 1 means product j satisfies aspect i.
     * Returns matchOfLeft[i] = product index assigned to aspect i, or -1.
     */
    /* [CO4] TOPIC: maximum bipartite matching by reducing to max-flow.
     * IN PROJECT: gives each requirement aspect its own distinct product (Recommendations menu). */
    public static int[] bipartiteMatch(int[][] adj, int L, int R) {
        int n = L + R + 2, s = L + R, t = L + R + 1;
        int[][] cap = new int[n][n];
        for (int i = 0; i < L; i++) cap[s][i] = 1;
        for (int j = 0; j < R; j++) cap[L + j][t] = 1;
        for (int i = 0; i < L; i++) for (int j = 0; j < R; j++) if (adj[i][j] == 1) cap[i][L + j] = 1;

        int[][] res = new int[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) res[i][j] = cap[i][j];
        int[] parent = new int[n];
        boolean[] vis = new boolean[n];
        while (true) {
            for (int i = 0; i < n; i++) { parent[i] = -1; vis[i] = false; }
            IntQueue q = new IntQueue(n);
            q.push(s); vis[s] = true;
            while (!q.empty()) {
                int u = q.pop();
                for (int v = 0; v < n; v++) if (!vis[v] && res[u][v] > 0) { vis[v] = true; parent[v] = u; q.push(v); }
            }
            if (!vis[t]) break;
            for (int v = t; v != s; v = parent[v]) { res[parent[v]][v] -= 1; res[v][parent[v]] += 1; }
        }
        int[] match = new int[L];
        for (int i = 0; i < L; i++) {
            match[i] = -1;
            for (int j = 0; j < R; j++) if (cap[i][L + j] == 1 && res[i][L + j] == 0) { match[i] = j; break; }
        }
        return match;
    }
}
