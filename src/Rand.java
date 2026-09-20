/* Rand - own Linear Congruential Generator (no java.util.Random). */
class Rand {
    long s;
    Rand(long seed) { s = seed; }
    int next(int n) { s = s * 6364136223846793005L + 1442695040888963407L; int v = (int) ((s >>> 33) % n); return v < 0 ? -v : v; }
}
