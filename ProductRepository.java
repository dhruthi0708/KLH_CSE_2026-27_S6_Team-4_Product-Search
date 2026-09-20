import java.io.*;

/*
 * ProductRepository - loads products.csv, builds the inverted keyword index
 * using the universal-hash map (CO6 randomised hashing).
 */
public class ProductRepository {
    public Product[] products;
    public int count = 0;
    public HashMapStr index;          // word -> list of product positions
    public StrList categories = new StrList();

    public void load(String path, Rand r) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line = br.readLine();                 // header
        Product[] tmp = new Product[5000];
        while ((line = br.readLine()) != null) {
            if (line.trim().length() == 0) continue;
            String[] f = splitCSV(line, 7);
            if (f == null) continue;
            try {
                Product p = new Product(Integer.parseInt(f[0].trim()), f[1], f[2], f[3], f[4],
                        Integer.parseInt(f[5].trim()), f[6]);
                if (count == tmp.length) { Product[] g = new Product[count * 2]; for (int i = 0; i < count; i++) g[i] = tmp[i]; tmp = g; }
                tmp[count++] = p;
                if (!categories.contains(p.category)) categories.add(p.category);
            } catch (NumberFormatException e) { /* skip bad row */ }
        }
        br.close();
        products = new Product[count];
        for (int i = 0; i < count; i++) products[i] = tmp[i];
        buildIndex(r);
    }

    /* manual CSV split (no String.split regex on the description commas beyond field 7) */
    static String[] splitCSV(String line, int n) {
        String[] out = new String[n];
        int start = 0, field = 0;
        for (int i = 0; i < line.length() && field < n - 1; i++) {
            if (line.charAt(i) == ',') { out[field++] = line.substring(start, i); start = i + 1; }
        }
        if (field != n - 1) return null;
        out[n - 1] = line.substring(start);           // last field keeps its commas
        return out;
    }

    /* Inverted index: every word of name+desc+brand+type mapped to product positions. */
    void buildIndex(Rand r) {
        index = new HashMapStr(40009, r);             // prime table size
        for (int i = 0; i < count; i++) {
            Product p = products[i];
            addWords(p.descLower, i);
            addWords(p.nameLower, i);
            addWords(p.type.toLowerCase(), i);
            addWords(p.brand.toLowerCase(), i);
        }
    }

    void addWords(String text, int pos) {
        StrList w = Text.tokenize(text);
        for (int i = 0; i < w.size(); i++) index.put(w.get(i), pos);
    }

    public Product byId(int id) {
        for (int i = 0; i < count; i++) if (products[i].id == id) return products[i];
        return null;
    }

    public int posOfId(int id) {
        for (int i = 0; i < count; i++) if (products[i].id == id) return i;
        return -1;
    }
}
