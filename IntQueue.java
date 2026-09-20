/* IntQueue - array based queue used by BFS in Edmonds-Karp. */
class IntQueue {
    int[] a; int head = 0, tail = 0;
    IntQueue(int cap) { a = new int[cap + 1]; }
    void push(int v) { a[tail++] = v; }
    int pop() { return a[head++]; }
    boolean empty() { return head == tail; }
}
