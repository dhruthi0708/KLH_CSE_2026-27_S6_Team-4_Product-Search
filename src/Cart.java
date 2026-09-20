import java.io.*;

/* Cart - stored per user in data/cart_<user>.csv */
class Cart {
    IntList ids = new IntList();
    IntList qty = new IntList();
    String user;
    ProductRepository cat;

    Cart(String user, ProductRepository cat) { this.user = user; this.cat = cat; load(); }

    String file() { return "data/cart_" + user + ".csv"; }

    void load() {
        try {
            File f = new File(file());
            if (!f.exists()) return;
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                int c = line.indexOf(',');
                if (c < 0) continue;
                ids.add(Integer.parseInt(line.substring(0, c).trim()));
                qty.add(Integer.parseInt(line.substring(c + 1).trim()));
            }
            br.close();
        } catch (Exception e) { }
    }

    void save() {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(file()));
            for (int i = 0; i < ids.size(); i++) pw.println(ids.get(i) + "," + qty.get(i));
            pw.close();
        } catch (Exception e) { }
    }

    void add(int id) {
        for (int i = 0; i < ids.size(); i++) if (ids.get(i) == id) { qty.set(i, qty.get(i) + 1); save(); return; }
        ids.add(id); qty.add(1); save();
    }

    boolean remove(int id) {
        for (int i = 0; i < ids.size(); i++)
            if (ids.get(i) == id) { ids.removeAt(i); qty.removeAt(i); save(); return true; }
        return false;
    }

    int totalItems() { int s = 0; for (int i = 0; i < qty.size(); i++) s += qty.get(i); return s; }

    long totalPrice() {
        long s = 0;
        for (int i = 0; i < ids.size(); i++) {
            Product p = cat.byId(ids.get(i));
            if (p != null) s += (long) p.price * qty.get(i);
        }
        return s;
    }
}
