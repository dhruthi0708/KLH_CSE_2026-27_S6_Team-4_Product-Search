class IntList {
    int[] a = new int[8];
    int n = 0;
    void add(int v) {
        if (n == a.length) { int[] b = new int[n * 2]; for (int i = 0; i < n; i++) b[i] = a[i]; a = b; }
        a[n++] = v;
    }
    int get(int i) { return a[i]; }
    void set(int i, int v) { a[i] = v; }
    int size() { return n; }
    boolean contains(int v) { for (int i = 0; i < n; i++) if (a[i] == v) return true; return false; }
    void removeAt(int i) { for (int j = i; j < n - 1; j++) a[j] = a[j + 1]; n--; }
    void clear() { n = 0; }
}
