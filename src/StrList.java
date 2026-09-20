class StrList {
    String[] a = new String[8];
    int n = 0;
    void add(String v) {
        if (n == a.length) { String[] b = new String[n * 2]; for (int i = 0; i < n; i++) b[i] = a[i]; a = b; }
        a[n++] = v;
    }
    String get(int i) { return a[i]; }
    int size() { return n; }
    boolean contains(String v) { for (int i = 0; i < n; i++) if (a[i].equals(v)) return true; return false; }
}
