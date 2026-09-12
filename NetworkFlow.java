public class NetworkFlow {

    private int[][] capacity;
    private int vertices;

    public NetworkFlow(int vertices) {
        this.vertices = vertices;
        capacity = new int[vertices][vertices];
    }

    // Add a connection with capacity
    public void addEdge(int from, int to, int value) {
        capacity[from][to] = value;
    }

    // BFS to find an augmenting path
    private boolean bfs(int[][] residual, int source, int sink, int[] parent) {

        boolean[] visited = new boolean[vertices];

        int[] queue = new int[vertices];
        int front = 0;
        int rear = 0;

        queue[rear++] = source;
        visited[source] = true;
        parent[source] = -1;

        while (front < rear) {

            int current = queue[front++];

            for (int next = 0; next < vertices; next++) {

                if (!visited[next] && residual[current][next] > 0) {

                    queue[rear++] = next;
                    parent[next] = current;
                    visited[next] = true;

                    if (next == sink) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // Ford-Fulkerson using BFS (Edmonds-Karp)
    public int maxFlow(int source, int sink) {

        int[][] residual = new int[vertices][vertices];

        for (int i = 0; i < vertices; i++) {
            for (int j = 0; j < vertices; j++) {
                residual[i][j] = capacity[i][j];
            }
        }

        int[] parent = new int[vertices];
        int maxFlow = 0;

        while (bfs(residual, source, sink, parent)) {

            int pathFlow = Integer.MAX_VALUE;

            int current = sink;

            while (current != source) {

                int previous = parent[current];

                pathFlow = Math.min(
                        pathFlow,
                        residual[previous][current]
                );

                current = previous;
            }

            current = sink;

            while (current != source) {

                int previous = parent[current];

                residual[previous][current] -= pathFlow;
                residual[current][previous] += pathFlow;

                current = previous;
            }

            maxFlow += pathFlow;
        }

        return maxFlow;
    }
}