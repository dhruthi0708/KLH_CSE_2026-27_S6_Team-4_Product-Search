/*
 * HashMapStr - CO6: randomised / universal hashing.
 * Bucket index = ((A*h + B) mod P) mod M with A,B picked randomly at build time.
 * This is the universal hash family h_{A,B} from the course (Session 22).
 */
class HashMapStr {
    static final long P = 1000000007L;
    long A, B;
    int M;
    String[] keys;
    IntList[] vals;
    int count = 0;

    HashMapStr(int m, Rand r) {
        M = m;
        keys = new String[M];
        vals = new IntList[M];
        A = 1 + r.next(1000000);   // A != 0
        B = r.next(1000000);
    }

    int hash(String s) {
        long h = 0;
        for (int i = 0; i < s.length(); i++) h = (h * 131 + s.charAt(i)) % P;   // polynomial hash
        long u = ((A * h + B) % P) % M;                                          // universal step
        return (int) u;
    }

    /* open addressing with linear probing */
    IntList bucket(String key, boolean create) {
        int i = hash(key);
        for (int probe = 0; probe < M; probe++) {
            int j = (i + probe) % M;
            if (keys[j] == null) {
                if (!create) return null;
                keys[j] = key; vals[j] = new IntList(); count++;
                return vals[j];
            }
            if (keys[j].equals(key)) return vals[j];
        }
        return null;
    }

    void put(String key, int v) { IntList b = bucket(key, true); if (b != null && !b.contains(v)) b.add(v); }
    IntList get(String key) { return bucket(key, false); }
}
