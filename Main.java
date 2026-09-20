import java.io.*;
import java.util.Scanner;   // only for reading console input

/*
 * Main.java - Product Search and Recommendation System for Shopping
 * Run:   javac -d bin src\*.java
 *        java -cp bin Main
 */
/*
 * CO INDEX (where each course outcome is used)
 *   CO1 ProblemClassifier            problem class -> algorithm family, measured evidence
 *   CO2 PatternSearch, SimilaritySearch   KMP, Z, Rabin-Karp, suffix array + LCP
 *   CO3 FuzzySearch, SimilaritySearch, GreedyRecommendation   edit distance, alignment, bitmask DP, knapsack
 *   CO4 NetworkFlow                  max-flow, min-cut, bipartite matching
 *   CO5 GreedyRecommendation         vertex cover 2-approximation, exact baseline, knapsack (NP-hard)
 *   CO6 ParallelSearch, HashMapStr   randomised quicksort, Miller-Rabin, reservoir sampling, parallel scan, universal hashing
 */
public class Main {

    static Scanner in = new Scanner(System.in);
    /* CO1 analysis + comparison counters are internal diagnostics; set true to print them (demo / viva). */
    static final boolean SHOW_ANALYSIS = false;
    static ProductRepository cat = new ProductRepository();
    static Rand rand = new Rand(System.nanoTime());
    static SearchEngine engine;
    static String user = null;
    static Cart cart = null;

    static String[] CATS = {"Electronics","Home Appliances","Beauty","Fashion","Grocery","Furniture",
        "Sports & Fitness","Books & Stationery","Automotive","Toys & Games","Kitchen","Footwear",
        "Bags & Accessories","Jewelry & Watches","All Products"};

    public static void main(String[] args) throws Exception {
        line();
        System.out.println("   PRODUCT SEARCH AND RECOMMENDATION SYSTEM FOR SHOPPING");
        line();
        System.out.print("Loading catalog ... ");
        cat.load("data/products.csv", rand);
        System.out.println(cat.count + " products loaded.");
        engine = new SearchEngine(cat, rand);

        authMenu();
        if (user == null) { System.out.println("Goodbye."); return; }
        cart = new Cart(user, cat);
        categoryMenu();
        System.out.println("\nThank you for shopping, " + user + "!");
    }

    static void line() { System.out.println("==========================================================="); }
    static void dash() { System.out.println("-----------------------------------------------------------"); }

    static String ask(String p) {
        System.out.print(p);
        if (!in.hasNextLine()) { System.out.println("\nInput ended. Exiting."); System.exit(0); }
        return in.nextLine().trim();
    }

    static int askInt(String p) {
        while (true) {
            String s = ask(p);
            try { return Integer.parseInt(s.trim()); } catch (Exception e) { System.out.println("  Please enter a number."); }
        }
    }

    /* ================= LOGIN / REGISTER ================= */
    static void authMenu() throws Exception {
        String prev = Auth.readSession();
        if (prev != null) {
            String c = ask("Active session found for '" + prev + "'. Continue as " + prev + "? (y/n): ");
            if (c.equalsIgnoreCase("y")) { user = prev; System.out.println("Welcome back, " + user + "!"); return; }
            Auth.clearSession();
        }
        while (user == null) {
            System.out.println("\n1. Login\n2. Register\n3. Exit");
            String c = ask("Choose: ");
            if (c.equals("1")) {
                String u = ask("Username: "), p = ask("Password: ");
                if (Auth.login(u, p)) { user = u; Auth.saveSession(u); System.out.println("Login successful. Session saved to data/session.txt"); }
                else System.out.println("Invalid username or password.");
            } else if (c.equals("2")) {
                String u = ask("New username: ");
                if (u.length() == 0 || u.indexOf(',') >= 0) { System.out.println("Invalid username."); continue; }
                String p = ask("New password: ");
                if (p.length() < 3) { System.out.println("Password too short."); continue; }
                if (Auth.register(u, p)) { user = u; Auth.saveSession(u); System.out.println("Registered and logged in."); }
                else System.out.println("Username already exists.");
            } else if (c.equals("3")) return;
            else System.out.println("Invalid choice.");
        }
    }

    /* ================= CATEGORY SELECTION ================= */
    static void categoryMenu() throws Exception {
        while (true) {
            line();
            System.out.println("  SELECT A CATEGORY");
            dash();
            for (int i = 0; i < CATS.length; i++)
                System.out.printf("%2d. %-22s%s", i + 1, CATS[i], (i % 2 == 1 || i == CATS.length - 1) ? "\n" : "");
            System.out.println(" 0. Logout and Exit");
            dash();
            int c = askInt("Choose category: ");
            if (c == 0) { Auth.clearSession(); return; }
            if (c < 1 || c > CATS.length) { System.out.println("Invalid choice."); continue; }
            if (productMenu(CATS[c - 1])) { Auth.clearSession(); return; }
        }
    }

    /* ================= PRODUCT MENU ================= */
    /* returns true if the user chose to exit the whole program */
    static boolean productMenu(String category) throws Exception {
        showProducts(category, 10);
        while (true) {
            line();
            System.out.println("  CATEGORY: " + category + "   |   Cart: " + cart.totalItems() + " item(s)");
            dash();
            System.out.println("1. Search by requirement / description   (main feature)");
            System.out.println("2. Select a product by ID (view details & add to cart)");
            System.out.println("3. Apply Filters");
            System.out.println("4. Recommendations");
            System.out.println("5. View Cart");
            System.out.println("6. Show more products in this category");
            System.out.println("7. Back to Categories");
            System.out.println("8. Exit");
            dash();
            String c = ask("Choose: ");
            if (c.equals("1")) { if (requirementSearch(category, 0, 0)) return true; }
            else if (c.equals("2")) { if (selectProductById(category)) return true; }
            else if (c.equals("3")) { if (filterMenu(category)) return true; }
            else if (c.equals("4")) { if (recommendMenu(category)) return true; }
            else if (c.equals("5")) { if (cartMenu()) return true; }
            else if (c.equals("6")) showProducts(category, 25);
            else if (c.equals("7")) return false;
            else if (c.equals("8")) return true;
            else System.out.println("Invalid choice.");
        }
    }

    /* ================= SELECT PRODUCT BY ID (no search needed) ================= */
    /* User picks an ID straight from the product list shown above, sees full
     * details, then Add to Cart / Leave / Back. Returns true if exit chosen. */
    static boolean selectProductById(String category) throws Exception {
        while (true) {
            String s = ask("Enter product ID to view (0 to go back): ");
            int id;
            try { id = Integer.parseInt(s); } catch (Exception e) { System.out.println("Enter a valid number."); continue; }
            if (id == 0) return false;
            Product p = cat.byId(id);
            if (p == null) { System.out.println("No product with that ID."); continue; }

            line();
            System.out.println("  COMPLETE PRODUCT DETAILS");
            dash();
            System.out.println("  Product ID : " + p.id);
            System.out.println("  Name       : " + p.name);
            System.out.println("  Brand      : " + p.brand);
            System.out.println("  Category   : " + p.category);
            System.out.println("  Type       : " + p.type);
            System.out.println("  Price      : Rs " + p.price);
            System.out.println("  Description:");
            printWrapped(p.desc, 68, "     ");
            dash();

            while (true) {
                System.out.println("1. Add to Cart\n2. Back to product list\n3. Leave (exit application)");
                String c = ask("Choose: ");
                if (c.equals("1")) { cart.add(p.id); System.out.println("Added. Cart now has " + cart.totalItems() + " item(s)."); break; }
                if (c.equals("2")) break;
                if (c.equals("3")) return true;
                System.out.println("Invalid choice.");
            }
        }
    }

    static void showProducts(String category, int limit) {
        dash();
        System.out.println("  PRODUCTS IN " + category.toUpperCase());
        dash();
        System.out.printf("%-6s %-38s %-16s %-10s%n", "ID", "NAME", "TYPE", "PRICE");
        int shown = 0;
        for (int i = 0; i < cat.count && shown < limit; i++) {
            Product p = cat.products[i];
            if (!category.equals("All Products") && !p.category.equals(category)) continue;
            System.out.printf("%-6d %-38s %-16s Rs %-10d%n", p.id, cut(p.name, 38), cut(p.type, 16), p.price);
            shown++;
        }
        System.out.println("  ... showing " + shown + " products. Use search to narrow down.");
    }

    static String cut(String s, int n) { return s.length() <= n ? s : s.substring(0, n - 1) + "."; }

    /* ================= FILTERS ================= */
    static boolean filterMenu(String category) throws Exception {
        System.out.println("\n-- APPLY FILTERS --");
        int min = askInt("Minimum price (0 for none): ");
        int max = askInt("Maximum price (0 for none): ");
        return requirementSearch(category, min, max);
    }

    /* ================= MAIN FEATURE : REQUIREMENT SEARCH ================= */
    /* [CO1][CO2][CO3][CO6] requirement search: classify query, suggest spelling fixes, score with string matching + similarity, rank */
    static boolean requirementSearch(String category, int min, int max) throws Exception {
        System.out.println("\nDescribe what you need. Example:");
        System.out.println("  \"lightweight laptop with long battery life for students under 45000\"");
        String q = ask("Your requirement: ");
        if (q.length() == 0) { System.out.println("Empty requirement."); return false; }

        String fixed = FuzzySearch.suggest(q, cat);                       // fuzzy search (CO3)
        if (fixed != null) {
            System.out.println("  Did you mean: \"" + fixed + "\" ? ");
            if (ask("  Use the corrected query? (y/n): ").equalsIgnoreCase("y")) q = fixed;
        }

        if (SHOW_ANALYSIS) System.out.println(ProblemClassifier.report(q, cat));   // CO1
        PatternSearch.naiveComparisons = 0; PatternSearch.kmpComparisons = 0;
        long t0 = System.currentTimeMillis();
        SearchEngine.Hit[] hits = engine.search(q, category, min, max, 15);
        long t1 = System.currentTimeMillis();

        if (hits.length == 0) {
            System.out.println("\nNo matching products found. Try different words or a wider budget.");
            return false;
        }
        hits = engine.diversify(hits, 10);
        StrList kw = Text.keywords(q);

        dash();
        System.out.println("  MATCHING PRODUCTS  (" + hits.length + " shown, searched in " + (t1 - t0) + " ms)");
        System.out.print("  Requirement keywords : ");
        for (int i = 0; i < kw.size(); i++) System.out.print(kw.get(i) + " ");
        int bud = Text.extractBudget(q);
        System.out.println(bud > 0 ? "\n  Detected budget      : Rs " + bud : "\n  Detected budget      : none");
        if (SHOW_ANALYSIS) System.out.println("  Character comparisons used: " + PatternSearch.kmpComparisons);
        dash();
        System.out.printf("%-6s %-34s %-12s %-9s %-6s %s%n", "ID", "NAME", "TYPE", "PRICE", "SCORE", "SUITABLE");
        for (SearchEngine.Hit h : hits) {
            Product p = engine.pr(h);
            System.out.printf("%-6d %-34s %-12s Rs %-7d %-6d %s%n",
                    p.id, cut(p.name, 34), cut(p.type, 12), p.price, h.score, h.verdict);
        }
        dash();

        while (true) {
            String s = ask("Enter product ID for full details (0 to go back): ");
            int id;
            try { id = Integer.parseInt(s); } catch (Exception e) { System.out.println("Enter a valid number."); continue; }
            if (id == 0) return false;
            SearchEngine.Hit sel = null;
            for (SearchEngine.Hit h : hits) if (engine.pr(h).id == id) { sel = h; break; }
            if (sel == null) { System.out.println("That ID is not in the result list above."); continue; }
            int r = productDetail(sel, kw);
            if (r == 2) return true;     // exit program
            if (r == 1) return false;    // back
        }
    }

    /* returns 0 = stay, 1 = back, 2 = exit */
    /* [CO2][CO3] "why this product" screen built from the match scores */
    static int productDetail(SearchEngine.Hit h, StrList kw) throws Exception {
        Product p = engine.pr(h);
        line();
        System.out.println("  COMPLETE PRODUCT DETAILS");
        dash();
        System.out.println("  Product ID : " + p.id);
        System.out.println("  Name       : " + p.name);
        System.out.println("  Brand      : " + p.brand);
        System.out.println("  Category   : " + p.category);
        System.out.println("  Type       : " + p.type);
        System.out.println("  Price      : Rs " + p.price);
        System.out.println("  Description:");
        printWrapped(p.desc, 68, "     ");
        System.out.println(engine.explain(h, kw));

        while (true) {
            System.out.println("1. Add to Cart\n2. Back to results\n3. Leave (exit application)");
            String c = ask("Choose: ");
            if (c.equals("1")) { cart.add(p.id); System.out.println("Added. Cart now has " + cart.totalItems() + " item(s)."); return 0; }
            if (c.equals("2")) return 0;
            if (c.equals("3")) return 2;
            System.out.println("Invalid choice.");
        }
    }

    static void printWrapped(String s, int w, String pad) {
        int i = 0;
        while (i < s.length()) {
            int end = Math.min(s.length(), i + w);
            if (end < s.length()) { int sp = s.lastIndexOf(' ', end); if (sp > i) end = sp; }
            System.out.println(pad + s.substring(i, end).trim());
            i = end;
        }
    }

    /* ================= RECOMMENDATIONS ================= */
    static boolean recommendMenu(String category) throws Exception {
        System.out.println("\n-- RECOMMENDATIONS --");
        System.out.println("1. Best bundle inside a budget");
        System.out.println("2. Match my requirement aspects");
        System.out.println("3. Surprise picks from this category");
        System.out.println("4. Similar to a product I liked");
        System.out.println("5. Back");
        String c = ask("Choose: ");
        if (c.equals("1")) budgetBundle(category);
        else if (c.equals("2")) aspectMatching(category);
        else if (c.equals("3")) surprisePicks(category);
        else if (c.equals("4")) similarProducts(category);
        return false;
    }

    /* Knapsack: choose the set of products with maximum total relevance within budget. */
    /* [CO3][CO5] best bundle inside a budget (0/1 knapsack) */
    static void budgetBundle(String category) throws Exception {
        String q = ask("What do you need (for relevance scoring)? ");
        int budget = askInt("Total budget in Rs: ");
        if (budget <= 0) { System.out.println("Budget must be positive."); return; }
        engine.typeMode = 2;                                   // a bundle may name several product types
        SearchEngine.Hit[] hits = engine.search(q, category, 0, budget, 20);
        engine.typeMode = 1;
        if (hits.length == 0) { System.out.println("Nothing relevant inside that budget."); return; }
        int n = Math.min(hits.length, 14);
        int[] val = new int[n], wt = new int[n];
        for (int i = 0; i < n; i++) { val[i] = Math.max(1, hits[i].score); wt[i] = engine.pr(hits[i]).price; }
        IntList picked = GreedyRecommendation.knapsackPick(val, wt, budget);
        dash();
        System.out.println("  BEST BUNDLE WITHIN Rs " + budget + "");
        dash();
        long total = 0; int relevance = 0;
        for (int i = picked.size() - 1; i >= 0; i--) {
            Product p = engine.pr(hits[picked.get(i)]);
            System.out.printf("  %-6d %-36s Rs %-8d relevance %d%n", p.id, cut(p.name, 36), p.price, val[picked.get(i)]);
            total += p.price; relevance += val[picked.get(i)];
        }
        System.out.println("  ------------------------------------------------");
        System.out.println("  Items: " + picked.size() + "   Total: Rs " + total + "   Budget left: Rs " + (budget - total));
        System.out.println("  Total relevance achieved: " + relevance + " (maximum possible for this budget)");
        String a = ask("Add this whole bundle to cart? (y/n): ");
        if (a.equalsIgnoreCase("y")) {
            for (int i = 0; i < picked.size(); i++) cart.add(engine.pr(hits[picked.get(i)]).id);
            System.out.println("Bundle added. Cart has " + cart.totalItems() + " item(s).");
        }
    }

    /* Bipartite matching: assign ONE distinct product to each requirement aspect. */
    /* [CO4] one distinct product per requirement aspect (bipartite matching via max-flow) */
    static void aspectMatching(String category) throws Exception {
        System.out.println("Enter your requirement aspects separated by commas.");
        System.out.println("Example: long battery, lightweight, good camera");
        String s = ask("Aspects: ");
        StrList aspects = new StrList();
        int st = 0;
        for (int i = 0; i <= s.length(); i++)
            if (i == s.length() || s.charAt(i) == ',') {
                String a = s.substring(st, i).trim();
                if (a.length() > 0) aspects.add(a);
                st = i + 1;
            }
        if (aspects.size() == 0) { System.out.println("No aspects given."); return; }

        engine.typeMode = 0;                                   // aspects are features, not product types
        SearchEngine.Hit[] pool = engine.search(s, category, 0, 0, 12);
        engine.typeMode = 1;
        if (pool.length == 0) { System.out.println("No candidate products."); return; }
        int L = aspects.size(), R = pool.length;
        int[][] adj = new int[L][R];
        for (int i = 0; i < L; i++) {
            StrList aw = Text.keywords(aspects.get(i));
            for (int j = 0; j < R; j++) {
                String blob = engine.pr(pool[j]).descLower;
                int hit = 0;
                for (int k = 0; k < aw.size(); k++) if (PatternSearch.kmpSearch(blob, aw.get(k)) >= 0) hit++;
                if (aw.size() > 0 && hit * 2 >= aw.size()) adj[i][j] = 1;   // product covers this aspect
            }
        }
        int[] match = NetworkFlow.bipartiteMatch(adj, L, R);
        dash();
        System.out.println("  ASPECT -> PRODUCT ASSIGNMENT");
        dash();
        int matched = 0;
        for (int i = 0; i < L; i++) {
            if (match[i] >= 0) {
                Product p = engine.pr(pool[match[i]]);
                System.out.printf("  %-28s -> %-34s Rs %d%n", cut(aspects.get(i), 28), cut(p.name, 34), p.price);
                matched++;
            } else {
                System.out.printf("  %-28s -> no distinct product covers this aspect%n", cut(aspects.get(i), 28));
            }
        }
        System.out.println("  Maximum matching size = " + matched + " of " + L + " aspects.");
    }

    /* Reservoir sampling over the category stream. */
    /* [CO6] random picks (reservoir sampling) */
    static void surprisePicks(String category) {
        IntList stream = new IntList();
        for (int i = 0; i < cat.count; i++)
            if (category.equals("All Products") || cat.products[i].category.equals(category)) stream.add(i);
        int[] pick = ParallelSearch.reservoirSample(stream, 5, rand);
        dash();
        System.out.println("  SURPRISE PICKS");
        dash();
        for (int p : pick) {
            Product pr = cat.products[p];
            System.out.printf("  %-6d %-38s Rs %-8d %s%n", pr.id, cut(pr.name, 38), pr.price, pr.type);
        }
    }

    /* Suffix-array based "more like this". */
    /* [CO2] similar products (suffix array / LCP similarity) */
    static void similarProducts(String category) {
        int id = askInt("Enter a product ID you liked: ");
        Product base = cat.byId(id);
        if (base == null) { System.out.println("No product with that ID."); return; }
        System.out.println("Finding products similar to: " + base.name);
        int best = 0;
        int[] score = new int[cat.count];
        IntList pool = new IntList();
        for (int i = 0; i < cat.count; i++) {
            Product p = cat.products[i];
            if (p.id == base.id) continue;
            if (!p.category.equals(base.category)) continue;
            pool.add(i);
        }
        int limit = Math.min(pool.size(), 120);
        int[] sc = new int[limit], idx = new int[limit];
        for (int i = 0; i < limit; i++) {
            int p = pool.get(i);
            int lcs = SimilaritySearch.longestCommonSubstring(base.descLower, cat.products[p].descLower);
            int priceClose = Math.max(0, 20 - Math.abs(cat.products[p].price - base.price) * 20 / Math.max(1, base.price));
            int sameType = cat.products[p].type.equals(base.type) ? 150 : 0;   // same type matters most
            int sameBrand = cat.products[p].brand.equals(base.brand) ? 15 : 0;
            sc[i] = lcs + priceClose + sameType + sameBrand;
            idx[i] = p;
        }
        ParallelSearch.randomizedQuickSort(sc, idx, 0, limit - 1, rand);
        dash();
        System.out.println("  SIMILAR PRODUCTS");
        dash();
        for (int i = 0; i < Math.min(5, limit); i++) {
            Product p = cat.products[idx[i]];
            System.out.printf("  %-6d %-38s Rs %-8d similarity %d%n", p.id, cut(p.name, 38), p.price, sc[i]);
        }
    }

    /* ================= CART ================= */
    /* [CO6] cart totals (parallel prefix-sum and reduce, checked against a sequential scan) */
    static boolean cartMenu() throws Exception {
        while (true) {
            dash();
            System.out.println("  YOUR CART (" + user + ")");
            dash();
            if (cart.ids.size() == 0) System.out.println("  Cart is empty.");
            else {
                System.out.printf("  %-6s %-34s %-6s %-10s %s%n", "ID", "NAME", "QTY", "PRICE", "SUBTOTAL");
                long[] amounts = new long[cart.ids.size()];
                for (int i = 0; i < cart.ids.size(); i++) {
                    Product p = cat.byId(cart.ids.get(i));
                    if (p == null) continue;
                    long sub = (long) p.price * cart.qty.get(i);
                    amounts[i] = sub;
                    System.out.printf("  %-6d %-34s %-6d Rs %-7d Rs %d%n", p.id, cut(p.name, 34), cart.qty.get(i), p.price, sub);
                }
                /* CO6: running totals with parallel prefix-sum, verified against sequential */
                try {
                    long[] par = ParallelSearch.parallelPrefixSum(amounts);
                    long[] seq = ParallelSearch.sequentialPrefixSum(amounts);
                    boolean same = true;
                    for (int i = 0; i < amounts.length; i++) if (par[i] != seq[i]) same = false;
                    System.out.println("  running total                       : Rs " + (par.length > 0 ? par[par.length - 1] : 0)
                            + (same ? "  [verified]" : "  [MISMATCH]"));
                    System.out.println("  cart total                          : Rs " + ParallelSearch.parallelReduce(amounts));
                } catch (Exception e) { }
                System.out.println("  ------------------------------------------------");
                System.out.println("  TOTAL ITEMS : " + cart.totalItems());
                System.out.println("  TOTAL PRICE : Rs " + cart.totalPrice());

                /* CO3: bitmask DP TSP to order pickup by shop section */
                if (cart.ids.size() >= 2 && cart.ids.size() <= 10) showPickupRoute();
            }
            dash();
            System.out.println("1. Add a product by ID\n2. Delete a product by ID\n3. Checkout (generate order ID)\n4. Continue shopping\n5. Back to categories\n6. Exit");
            String c = ask("Choose: ");
            if (c.equals("1")) {
                int id = askInt("Product ID to add: ");
                if (cat.byId(id) == null) System.out.println("No such product.");
                else { cart.add(id); System.out.println("Added."); }
            } else if (c.equals("2")) {
                int id = askInt("Product ID to delete: ");
                System.out.println(cart.remove(id) ? "Removed." : "That product is not in the cart.");
            } else if (c.equals("3")) checkout();
            else if (c.equals("4")) return false;
            else if (c.equals("5")) return false;
            else if (c.equals("6")) return true;
            else System.out.println("Invalid choice.");
        }
    }

    /* Bitmask DP (TSP) over cart items grouped by category "section" of the store. */
    /* [CO3] shortest store pickup route (bitmask DP travelling salesperson) */
    static void showPickupRoute() {
        int n = cart.ids.size();
        int[] sec = new int[n];
        for (int i = 0; i < n; i++) {
            Product p = cat.byId(cart.ids.get(i));
            sec[i] = 0;
            for (int c = 0; c < CATS.length; c++) if (p != null && CATS[c].equals(p.category)) sec[i] = c;
        }
        int[][] dist = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) dist[i][j] = Math.abs(sec[i] - sec[j]) + 1;
        int[] order = GreedyRecommendation.tspOrder(dist);
        System.out.print("  shortest store pickup route: ");
        for (int i = 0; i < order.length; i++) {
            Product p = cat.byId(cart.ids.get(order[i]));
            System.out.print((p == null ? "?" : p.category) + (i < order.length - 1 ? " -> " : "\n"));
        }
    }

    /* Checkout: order ID is validated as prime with Miller-Rabin (CO6). */
    /* [CO6] prime order ID (Miller-Rabin) */
    static void checkout() {
        if (cart.ids.size() == 0) { System.out.println("Cart is empty."); return; }
        long candidate = 100003 + rand.next(900000);
        int tries = 0;
        while (!ParallelSearch.millerRabin(candidate, 8, rand) && tries < 20000) { candidate++; tries++; }
        System.out.println("\n  ORDER PLACED");
        System.out.println("  Order ID   : ORD-" + candidate + "  (prime, verified)");
        System.out.println("  Items      : " + cart.totalItems());
        System.out.println("  Amount     : Rs " + cart.totalPrice());
        System.out.println("  Customer   : " + user);
        try {
            PrintWriter pw = new PrintWriter(new FileWriter("data/orders.txt", true));
            pw.println("ORD-" + candidate + "," + user + "," + cart.totalItems() + "," + cart.totalPrice());
            pw.close();
        } catch (Exception e) { }
        cart.ids.clear(); cart.qty.clear(); cart.save();
        System.out.println("  Order saved to data/orders.txt. Cart cleared.");
    }
}
