import java.io.*;

/* Wishlist - per user, stored in data/wishlist_<user>.csv (one product id per line) */
class Wishlist {
    IntList ids = new IntList(); String user;
    Wishlist(String user) {
        this.user = user;
        try {
            File f = new File(file()); if (!f.exists()) return;
            BufferedReader br = new BufferedReader(new FileReader(f)); String line;
            while ((line = br.readLine()) != null) { try { ids.add(Integer.parseInt(line.trim())); } catch (Exception e) { } }
            br.close();
        } catch (Exception e) { }
    }
    String file() { return "data/wishlist_" + user + ".csv"; }
    void save() {
        try { PrintWriter pw = new PrintWriter(new FileWriter(file())); for (int i = 0; i < ids.size(); i++) pw.println(ids.get(i)); pw.close(); } catch (Exception e) { }
    }
    boolean add(int id) { if (ids.contains(id)) return false; ids.add(id); save(); return true; }
    boolean remove(int id) { for (int i = 0; i < ids.size(); i++) if (ids.get(i) == id) { ids.removeAt(i); save(); return true; } return false; }
    int[] toArray() { int[] r = new int[ids.size()]; for (int i = 0; i < r.length; i++) r[i] = ids.get(i); return r; }
}

/* SearchHistory - most-recent-first bounded list, stored in data/history_<user>.txt */
class SearchHistory {
    static final int MAX = 15;
    String[] q = new String[MAX]; int n = 0; String user;
    SearchHistory(String user) {
        this.user = user;
        try {
            File f = new File(file()); if (!f.exists()) return;
            BufferedReader br = new BufferedReader(new FileReader(f)); String line;
            while ((line = br.readLine()) != null && n < MAX) { line = line.trim(); if (line.length() > 0) q[n++] = line; }
            br.close();
        } catch (Exception e) { }
    }
    String file() { return "data/history_" + user + ".txt"; }
    void add(String s) {
        s = s.trim(); if (s.length() == 0) return;
        int at = -1;
        for (int i = 0; i < n; i++) if (q[i].equalsIgnoreCase(s)) { at = i; break; }
        if (at < 0) { at = Math.min(n, MAX - 1); if (n < MAX) n++; }
        for (int i = at; i > 0; i--) q[i] = q[i - 1];   // shift down, newest goes to the front
        q[0] = s; save();
    }
    void clear() { n = 0; save(); }
    void save() {
        try { PrintWriter pw = new PrintWriter(new FileWriter(file())); for (int i = 0; i < n; i++) pw.println(q[i]); pw.close(); } catch (Exception e) { }
    }
}

/* One past order. detail = "productId:qty;productId:qty" (empty for old orders without item records) */
class OrderRec { String id, date = "-", detail = ""; int items; long amount; }

/* OrderLog - orders.txt (existing format) + data/order_items.csv (new: id,user,date,detail) */
class OrderLog {
    static void append(String id, String user, String date, String detail) {
        try { PrintWriter pw = new PrintWriter(new FileWriter("data/order_items.csv", true)); pw.println(id + "," + user + "," + date + "," + detail); pw.close(); } catch (Exception e) { }
    }
    /* returns the user's orders in the order they were placed (oldest first) */
    static OrderRec[] load(String user) {
        OrderRec[] r = new OrderRec[16]; int n = 0;
        try {
            File f = new File("data/orders.txt");
            if (f.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(f)); String line;
                while ((line = br.readLine()) != null) {
                    String[] p = line.trim().split(",");
                    if (p.length < 4 || !p[1].equals(user)) continue;
                    OrderRec o = new OrderRec(); o.id = p[0];
                    try { o.items = Integer.parseInt(p[2].trim()); o.amount = Long.parseLong(p[3].trim()); } catch (Exception e) { continue; }
                    if (n == r.length) { OrderRec[] b = new OrderRec[n * 2]; for (int i = 0; i < n; i++) b[i] = r[i]; r = b; }
                    r[n++] = o;
                }
                br.close();
            }
            f = new File("data/order_items.csv");
            if (f.exists() && n > 0) {
                BufferedReader br = new BufferedReader(new FileReader(f)); String line;
                while ((line = br.readLine()) != null) {
                    String[] p = line.trim().split(",", 4);
                    if (p.length < 4) continue;
                    for (int i = 0; i < n; i++) if (r[i].id.equals(p[0])) { r[i].date = p[2]; r[i].detail = p[3]; }
                }
                br.close();
            }
        } catch (Exception e) { }
        OrderRec[] out = new OrderRec[n]; for (int i = 0; i < n; i++) out[i] = r[i];
        return out;
    }
}
