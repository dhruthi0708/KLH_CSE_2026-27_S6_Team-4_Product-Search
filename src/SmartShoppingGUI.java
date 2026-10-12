import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/*
 * SmartShoppingGUI - Swing front-end. All DSA logic (SearchEngine, FuzzySearch, NetworkFlow,
 * GreedyRecommendation, ParallelSearch, Auth, Cart, ...) is reused unchanged.
 * Run from the project folder (the one containing data/):
 *    javac -d bin src/*.java
 *    java -cp bin SmartShoppingGUI
 */
public class SmartShoppingGUI extends JFrame {
    static final Color BG = new Color(0xF4F6FA), CARD = Color.WHITE, PRIMARY = new Color(0x4F46E5),
            DARK = new Color(0x1E1B4B), TEXT = new Color(0x1F2937), MUTED = new Color(0x6B7280), GREEN = new Color(0x059669), RED = new Color(0xDC2626);
    static final Font F = new Font("Segoe UI", Font.PLAIN, 14), FB = new Font("Segoe UI", Font.BOLD, 14),
            FT = new Font("Segoe UI", Font.BOLD, 24), MONO = new Font("Consolas", Font.PLAIN, 13);

    static final String[] CATS = {"Electronics","Home Appliances","Beauty","Fashion","Grocery","Furniture",
        "Sports & Fitness","Books & Stationery","Automotive","Toys & Games","Kitchen","Footwear",
        "Bags & Accessories","Jewelry & Watches","All Products"};

    ProductRepository cat = new ProductRepository();
    Rand rand = new Rand(System.nanoTime());
    SearchEngine engine;
    String user;
    Cart cart;

    CardLayout root = new CardLayout();
    JPanel rootPanel = new JPanel(root);
    JLabel userLbl = new JLabel(), cartLbl = new JLabel();
    JComboBox<String> catBox = new JComboBox<>(CATS);
    JTabbedPane tabs = new JTabbedPane();
    DefaultTableModel cartModel;
    JTable cartTable;
    SearchEngine.Hit[] lastHits; StrList lastKw;
    JLabel searchInfo = new JLabel(" ");
    JTextField qField = new JTextField(), minField = new JTextField("0", 6), maxField = new JTextField("0", 6);
    JLabel cartTotals = new JLabel(" "), cartRoute = new JLabel(" ");

    public static void main(String[] a) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) { }
            try { new SmartShoppingGUI().setVisible(true); }
            catch (Exception e) { JOptionPane.showMessageDialog(null, "Could not start: " + e + "\nRun from the project folder that contains data/products.csv", "Error", JOptionPane.ERROR_MESSAGE); }
        });
    }

    SmartShoppingGUI() throws Exception {
        super("Smart Shopping - Product Search & Recommendation");
        cat.load("data/products.csv", rand);
        engine = new SearchEngine(cat, rand);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720); setLocationRelativeTo(null);
        rootPanel.add(loginPanel(), "login");
        rootPanel.add(shopPanel(), "shop");
        setContentPane(rootPanel);
        String prev = Auth.readSession();
        if (prev != null && JOptionPane.showConfirmDialog(this, "Active session found for '" + prev + "'. Continue as " + prev + "?", "Session", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            enterShop(prev);
        } else { if (prev != null) Auth.clearSession(); root.show(rootPanel, "login"); }
    }

    /* ---------- helpers ---------- */
    JButton btn(String t, Color c, ActionListener l) {
        JButton b = new JButton(t) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? c.darker() : c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setFont(FB); b.setForeground(Color.WHITE); b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setFocusPainted(false); b.setCursor(new Cursor(Cursor.HAND_CURSOR)); b.setBorder(new EmptyBorder(9, 18, 9, 18));
        b.addActionListener(l); return b;
    }
    JPanel card(LayoutManager lm) { JPanel p = new JPanel(lm); p.setBackground(CARD); p.setBorder(new CompoundBorder(new LineBorder(new Color(0xE5E7EB)), new EmptyBorder(14, 16, 14, 16))); return p; }
    JLabel label(String t, Font f, Color c) { JLabel l = new JLabel(t); l.setFont(f); l.setForeground(c); return l; }
    void style(JTextField t) { t.setFont(F); t.setBorder(new CompoundBorder(new LineBorder(new Color(0xD1D5DB)), new EmptyBorder(6, 8, 6, 8))); }
    JTable table(DefaultTableModel m) {
        JTable t = new JTable(m) { public boolean isCellEditable(int r, int c) { return false; } };
        t.setFont(F); t.setRowHeight(28); t.setSelectionBackground(new Color(0xE0E7FF)); t.setSelectionForeground(TEXT);
        t.setShowGrid(false); t.setIntercellSpacing(new Dimension(0, 0));
        t.getTableHeader().setFont(FB); t.getTableHeader().setBackground(DARK); t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setPreferredSize(new Dimension(0, 32));
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            { setOpaque(true); }
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tb, v, sel, foc, r, c);
                setBackground(DARK); setForeground(Color.WHITE); setFont(FB);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        });
        return t;
    }
    JScrollPane scroll(Component c) { JScrollPane s = new JScrollPane(c); s.setBorder(new LineBorder(new Color(0xE5E7EB))); s.getViewport().setBackground(Color.WHITE); return s; }
    void msg(String s) { JOptionPane.showMessageDialog(this, s); }
    boolean yes(String s) { return JOptionPane.showConfirmDialog(this, s, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION; }
    static String cut(String s, int n) { return s.length() <= n ? s : s.substring(0, n - 1) + "."; }
    String category() { return (String) catBox.getSelectedItem(); }
    JTextArea out() { JTextArea a = new JTextArea(); a.setFont(MONO); a.setEditable(false); a.setMargin(new Insets(8, 10, 8, 10)); return a; }
    int num(String s, int def) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; } }

    /* ---------- login ---------- */
    JPanel loginPanel() {
        JPanel bg = new JPanel(new GridBagLayout()); bg.setBackground(DARK);
        JPanel c = new JPanel(); c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS)); c.setBackground(CARD);
        c.setBorder(new EmptyBorder(30, 40, 30, 40));
        JLabel t = label("Smart Shopping", FT, DARK); t.setAlignmentX(0);
        JLabel s = label("Product Search & Recommendation System", F, MUTED); s.setAlignmentX(0);
        JTextField u = new JTextField(20); style(u); JPasswordField p = new JPasswordField(20); p.setFont(F);
        p.setBorder(u.getBorder());
        JLabel lu = label("Username", FB, TEXT), lp = label("Password", FB, TEXT); lu.setAlignmentX(0); lp.setAlignmentX(0);
        u.setAlignmentX(0); p.setAlignmentX(0);
        u.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36)); p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        ActionListener login = e -> {
            try {
                String us = u.getText().trim(), pw = new String(p.getPassword());
                if (Auth.login(us, pw)) { Auth.saveSession(us); enterShop(us); p.setText(""); }
                else msg("Invalid username or password.");
            } catch (Exception ex) { msg("Error: " + ex.getMessage()); }
        };
        ActionListener reg = e -> {
            try {
                String us = u.getText().trim(), pw = new String(p.getPassword());
                if (us.length() == 0 || us.indexOf(',') >= 0) { msg("Invalid username."); return; }
                if (pw.length() < 3) { msg("Password too short (min 3)."); return; }
                if (Auth.register(us, pw)) { Auth.saveSession(us); enterShop(us); p.setText(""); }
                else msg("Username already exists.");
            } catch (Exception ex) { msg("Error: " + ex.getMessage()); }
        };
        p.addActionListener(login);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); row.setOpaque(false); row.setAlignmentX(0);
        JButton b1 = btn("Login", PRIMARY, login), b2 = btn("Register", GREEN, reg);
        row.add(b1); row.add(Box.createHorizontalStrut(10)); row.add(b2);
        c.add(t); c.add(s); c.add(Box.createVerticalStrut(22)); c.add(lu); c.add(Box.createVerticalStrut(4)); c.add(u);
        c.add(Box.createVerticalStrut(12)); c.add(lp); c.add(Box.createVerticalStrut(4)); c.add(p);
        c.add(Box.createVerticalStrut(20)); c.add(row);
        bg.add(c); return bg;
    }

    void enterShop(String u) {
        user = u; cart = new Cart(u, cat);
        userLbl.setText("Hello, " + u);
        wish = new Wishlist(u); hist = new SearchHistory(u); refreshHist();
        refreshBrowse(); refreshCart(); refreshWish(); refreshOrders();
        root.show(rootPanel, "shop");
    }

    void logout() { try { Auth.clearSession(); } catch (Exception e) { } user = null; root.show(rootPanel, "login"); }

    /* ---------- shop shell ---------- */
    JPanel shopPanel() {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(BG);
        JPanel top = new JPanel(new BorderLayout()); top.setBackground(DARK); top.setBorder(new EmptyBorder(12, 18, 12, 18));
        top.add(label("Smart Shopping", new Font("Segoe UI", Font.BOLD, 20), Color.WHITE), BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0)); right.setOpaque(false);
        userLbl.setFont(F); userLbl.setForeground(Color.WHITE); cartLbl.setFont(FB); cartLbl.setForeground(new Color(0xFCD34D));
        catBox.setFont(F); catBox.addActionListener(e -> { if (user != null) refreshBrowse(); });
        right.add(label("Category:", F, Color.WHITE)); right.add(catBox); right.add(cartLbl); right.add(userLbl);
        right.add(btn("Logout", RED, e -> logout()));
        top.add(right, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);
        tabs.setFont(FB);
        tabs.addTab("Browse", browsePanel()); tabs.addTab("Search", searchPanel());
        tabs.addTab("Recommendations", recoPanel()); tabs.addTab("Wishlist", wishPanel());
        tabs.addTab("Cart", cartPanel()); tabs.addTab("Orders", ordersPanel());
        tabs.addChangeListener(e -> { if (user != null) { refreshWish(); refreshOrders(); } });
        JPanel w = new JPanel(new BorderLayout()); w.setBorder(new EmptyBorder(12, 14, 14, 14)); w.setOpaque(false); w.add(tabs);
        p.add(w); return p;
    }

    /* ---------- shared product view: sorting + pagination + actions ---------- */
    static final String[] SORTS = {"Default order", "Price: Low to High", "Price: High to Low", "Name: A to Z", "Name: Z to A"};
    ProductView browseView, searchView, wishView;
    Wishlist wish; SearchHistory hist;

    class ProductView extends JPanel {
        final String mode;                       // "browse", "search" or "wish"
        int[] ids = new int[0], score; String[] verdict; int[] perm = new int[0];
        int page = 0, pageSize = 25;
        DefaultTableModel model; JTable table;
        JComboBox<String> sortBox = new JComboBox<>(SORTS), sizeBox = new JComboBox<>(new String[]{"10", "25", "50", "100"});
        JLabel pageLbl = new JLabel(" "); JButton prev, next;

        ProductView(String mode) {
            super(new BorderLayout(0, 8)); this.mode = mode; setOpaque(false);
            boolean s = mode.equals("search");
            model = new DefaultTableModel(s ? new String[]{"ID", "Name", "Type", "Price (Rs)", "Score", "Suitable"} : new String[]{"ID", "Name", "Brand", "Type", "Price (Rs)"}, 0);
            table = table(model);
            table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            table.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) details(); } });
            sortBox.setFont(F); sizeBox.setFont(F); sizeBox.setSelectedItem("25");
            sortBox.addActionListener(e -> applySort());
            sizeBox.addActionListener(e -> { pageSize = Integer.parseInt((String) sizeBox.getSelectedItem()); page = 0; render(); });
            JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); bar.setOpaque(false);
            bar.add(btn("Details", PRIMARY, e -> details()));
            bar.add(btn("Add to cart", GREEN, e -> addCart()));
            bar.add(btn(mode.equals("wish") ? "Remove from wishlist" : "Add to wishlist", new Color(0xDB2777), e -> wishAction()));
            bar.add(btn("Compare (2-4)", new Color(0x0891B2), e -> compare(selectedIds())));
            bar.add(label("Sort:", FB, TEXT)); bar.add(sortBox);
            JPanel foot = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0)); foot.setOpaque(false);
            prev = btn("< Prev", DARK, e -> { if (page > 0) { page--; render(); } });
            next = btn("Next >", DARK, e -> { if ((page + 1) * pageSize < ids.length) { page++; render(); } });
            foot.add(prev); foot.add(pageLbl); foot.add(next); foot.add(label("Rows per page:", F, MUTED)); foot.add(sizeBox);
            pageLbl.setFont(FB);
            add(bar, BorderLayout.NORTH); add(scroll(table)); add(foot, BorderLayout.SOUTH);
        }
        void setData(int[] ids, int[] score, String[] verdict) { this.ids = ids; this.score = score; this.verdict = verdict; applySort(); }
        void applySort() {
            int n = ids.length, k = sortBox.getSelectedIndex();
            if (k == 0) { perm = new int[n]; for (int i = 0; i < n; i++) perm[i] = i; }
            else if (k <= 2) { long[] pr = new long[n]; for (int i = 0; i < n; i++) pr[i] = cat.byId(ids[i]).price; perm = Sorter.byLong(pr, k == 1); }
            else { String[] nm = new String[n]; for (int i = 0; i < n; i++) nm[i] = cat.byId(ids[i]).name; perm = Sorter.byString(nm, k == 3); }
            page = 0; render();
        }
        void render() {
            model.setRowCount(0);
            int n = ids.length, pages = Math.max(1, (n + pageSize - 1) / pageSize);
            if (page >= pages) page = pages - 1;
            int a = page * pageSize, b = Math.min(n, a + pageSize);
            for (int i = a; i < b; i++) {
                int pos = perm[i]; Product p = cat.byId(ids[pos]);
                if (mode.equals("search")) model.addRow(new Object[]{p.id, p.name, p.type, p.price, score[pos], verdict[pos]});
                else model.addRow(new Object[]{p.id, p.name, p.brand, p.type, p.price});
            }
            pageLbl.setText(n == 0 ? "No products" : "Page " + (page + 1) + " / " + pages + "   (showing " + (a + 1) + "-" + b + " of " + n + ")");
            prev.setEnabled(page > 0); next.setEnabled(page < pages - 1);
        }
        int[] selectedIds() {
            int[] rows = table.getSelectedRows(), r = new int[rows.length];
            for (int i = 0; i < rows.length; i++) r[i] = (Integer) table.getValueAt(rows[i], 0);
            return r;
        }
        Product one() {
            int r = table.getSelectedRow(); if (r < 0) { msg("Select a product first."); return null; }
            return cat.byId((Integer) table.getValueAt(r, 0));
        }
        void details() {
            Product p = one(); if (p == null) return;
            String ex = null;
            if (mode.equals("search") && lastHits != null) ex = engine.explain(lastHits[perm[page * pageSize + table.getSelectedRow()]], lastKw);
            showDetailDialog(p, ex);
        }
        void addCart() {
            int[] s = selectedIds(); if (s.length == 0) { msg("Select a product first."); return; }
            for (int id : s) cart.add(id);
            refreshCart(); msg(s.length + " product(s) added. Cart now has " + cart.totalItems() + " item(s).");
        }
        void wishAction() {
            int[] s = selectedIds(); if (s.length == 0) { msg("Select a product first."); return; }
            if (mode.equals("wish")) { for (int id : s) wish.remove(id); refreshWish(); }
            else { int c = 0; for (int id : s) if (wish.add(id)) c++; msg(c + " product(s) added to wishlist (" + wish.ids.size() + " total)."); refreshWish(); }
        }
    }

    int[] toArr(IntList l) { int[] r = new int[l.size()]; for (int i = 0; i < r.length; i++) r[i] = l.get(i); return r; }

    /* ---------- browse ---------- */
    JPanel browsePanel() { browseView = new ProductView("browse"); return browseView; }
    void refreshBrowse() {
        String c = category(); IntList l = new IntList();
        for (int i = 0; i < cat.count; i++) { Product p = cat.products[i]; if (c.equals("All Products") || p.category.equals(c)) l.add(p.id); }
        browseView.setData(toArr(l), null, null); updateCartLbl();
    }
    void updateCartLbl() { cartLbl.setText(cart == null ? "" : "Cart: " + cart.totalItems() + " item(s)"); }
    String details(Product p) {
        return "Product ID : " + p.id + "\nName       : " + p.name + "\nBrand      : " + p.brand + "\nCategory   : " + p.category
             + "\nType       : " + p.type + "\nPrice      : Rs " + p.price + "\n\nDescription:\n" + p.desc;
    }
    void showDetailDialog(Product p, String extra) {
        JTextArea a = out(); a.setLineWrap(true); a.setWrapStyleWord(true);
        a.setText(details(p) + (extra == null ? "" : "\n\n" + extra));
        JScrollPane sp = scroll(a); sp.setPreferredSize(new Dimension(560, 380));
        Object[] opts = {"Add to Cart", "Add to Wishlist", "Close"};
        int r = JOptionPane.showOptionDialog(this, sp, "Product Details", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[2]);
        if (r == 0) { cart.add(p.id); refreshCart(); }
        else if (r == 1) { wish.add(p.id); refreshWish(); }
    }

    /* ---------- compare ---------- */
    void compare(int[] sel) {
        if (sel.length < 2 || sel.length > 4) { msg("Select 2 to 4 products to compare (Ctrl+click or Shift+click rows)."); return; }
        int n = sel.length; Product[] ps = new Product[n]; long low = Long.MAX_VALUE;
        for (int i = 0; i < n; i++) { ps[i] = cat.byId(sel[i]); if (ps[i].price < low) low = ps[i].price; }
        String[] cols = new String[n + 1]; cols[0] = "Attribute"; for (int i = 0; i < n; i++) cols[i + 1] = "Product " + (i + 1);
        String[] attrs = {"Name", "Brand", "Category", "Type", "Price (Rs)", "Price vs cheapest", "Key features", "Similarity to Product 1"};
        Object[][] data = new Object[attrs.length][n + 1];
        for (int r = 0; r < attrs.length; r++) data[r][0] = attrs[r];
        for (int i = 0; i < n; i++) {
            Product p = ps[i]; int c = i + 1;
            data[0][c] = "<html><b>" + p.name + "</b> (ID " + p.id + ")</html>"; data[1][c] = p.brand; data[2][c] = p.category; data[3][c] = p.type;
            data[4][c] = p.price + (p.price == low ? "  (lowest)" : "");
            data[5][c] = p.price == low ? "-" : "+Rs " + (p.price - low) + " (" + ((p.price - low) * 100 / Math.max(1, low)) + "% more)";
            int a = p.desc.indexOf("It offers "), b = p.desc.indexOf(". Ideal");
            data[6][c] = "<html>" + (a >= 0 && b > a ? p.desc.substring(a + 10, b) : p.desc) + "</html>";
            data[7][c] = i == 0 ? "-" : SimilaritySearch.longestCommonSubstring(ps[0].descLower, p.descLower) + " matching chars in description";
        }
        JTable t = table(new DefaultTableModel(data, cols));
        t.setRowHeight(48); t.getColumnModel().getColumn(0).setPreferredWidth(140);
        JScrollPane sp = scroll(t); sp.setPreferredSize(new Dimension(Math.min(1000, 160 + n * 220), 400));
        JOptionPane.showMessageDialog(this, sp, "Product Comparison", JOptionPane.PLAIN_MESSAGE);
    }

    /* ---------- wishlist ---------- */
    JPanel wishPanel() { wishView = new ProductView("wish"); return wishView; }
    void refreshWish() { if (wish != null && wishView != null) wishView.setData(wish.toArray(), null, null); }

    /* ---------- search ---------- */
    JComboBox<String> histBox = new JComboBox<>();
    boolean histUpdating = false;
    JPanel searchPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 10)); p.setBackground(BG);
        JPanel in = card(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(4, 4, 4, 4); g.fill = GridBagConstraints.HORIZONTAL;
        style(qField); style(minField); style(maxField);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 5; in.add(label("Describe what you need  (e.g. \"lightweight laptop with long battery life for students under 45000\")", FB, TEXT), g);
        g.gridy = 1; g.weightx = 1; in.add(qField, g);
        g.gridy = 2; g.gridwidth = 1; g.weightx = 0;
        in.add(label("Min price", F, MUTED), g); g.gridx = 1; in.add(minField, g);
        g.gridx = 2; in.add(label("Max price", F, MUTED), g); g.gridx = 3; in.add(maxField, g);
        g.gridx = 4; in.add(btn("Search", PRIMARY, e -> doSearch()), g);
        histBox.setFont(F);
        histBox.addActionListener(e -> {
            if (histUpdating || histBox.getSelectedIndex() <= 0) return;
            qField.setText((String) histBox.getSelectedItem()); doSearch();
        });
        g.gridx = 0; g.gridy = 3; g.gridwidth = 4; g.weightx = 1; in.add(histBox, g);
        g.gridx = 4; g.gridwidth = 1; g.weightx = 0; in.add(btn("Clear history", RED, e -> { if (hist != null) { hist.clear(); refreshHist(); } }), g);
        qField.addActionListener(e -> doSearch());
        searchInfo.setFont(F); searchInfo.setForeground(MUTED);
        JPanel north = new JPanel(new BorderLayout(0, 6)); north.setOpaque(false); north.add(in); north.add(searchInfo, BorderLayout.SOUTH);
        searchView = new ProductView("search");
        p.add(north, BorderLayout.NORTH); p.add(searchView); return p;
    }
    void refreshHist() {
        histUpdating = true; histBox.removeAllItems();
        histBox.addItem(hist == null || hist.n == 0 ? "Recent searches: (none yet)" : "Recent searches (" + hist.n + ") - pick one to search again");
        if (hist != null) for (int i = 0; i < hist.n; i++) histBox.addItem(hist.q[i]);
        histBox.setSelectedIndex(0); histUpdating = false;
    }
    void doSearch() {
        try {
            String q = qField.getText().trim();
            if (q.length() == 0) { msg("Empty requirement."); return; }
            int min = num(minField.getText(), 0), max = num(maxField.getText(), 0);
            String fixed = FuzzySearch.suggest(q, cat);
            if (fixed != null && yes("Did you mean: \"" + fixed + "\" ?\nUse the corrected query?")) { q = fixed; qField.setText(q); }
            PatternSearch.naiveComparisons = 0; PatternSearch.kmpComparisons = 0;
            long t0 = System.currentTimeMillis();
            SearchEngine.Hit[] hits = engine.search(q, category(), min, max, 60);
            long t1 = System.currentTimeMillis();
            lastHits = null;
            if (hits.length == 0) { searchView.setData(new int[0], new int[0], new String[0]); searchInfo.setText("No matching products found. Try different words or a wider budget."); return; }
            hits = engine.diversify(hits, Math.min(hits.length, 50));
            lastHits = hits; lastKw = Text.keywords(q);
            hist.add(q); refreshHist();
            StringBuilder kw = new StringBuilder();
            for (int i = 0; i < lastKw.size(); i++) kw.append(lastKw.get(i)).append(' ');
            int bud = Text.extractBudget(q);
            searchInfo.setText(hits.length + " results, searched in " + (t1 - t0) + " ms   |   Keywords: " + kw + "  |   Budget: " + (bud > 0 ? "Rs " + bud : "none"));
            int[] ids = new int[hits.length], sc = new int[hits.length]; String[] vd = new String[hits.length];
            for (int i = 0; i < hits.length; i++) { ids[i] = engine.pr(hits[i]).id; sc[i] = hits[i].score; vd[i] = String.valueOf(hits[i].verdict); }
            searchView.setData(ids, sc, vd);
        } catch (Exception ex) { msg("Search error: " + ex); }
    }

    /* ---------- order history ---------- */
    DefaultTableModel orderModel; JTable orderTable; JTextArea orderDetail = out();
    OrderRec[] orders = new OrderRec[0]; int[] orderPerm = new int[0];
    JComboBox<String> orderSort = new JComboBox<>(new String[]{"Newest first", "Oldest first", "Amount: High to Low", "Amount: Low to High"});
    JPanel ordersPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8)); p.setBackground(BG);
        orderModel = new DefaultTableModel(new String[]{"Order ID", "Date", "Items", "Amount (Rs)"}, 0);
        orderTable = table(orderModel); orderSort.setFont(F);
        orderSort.addActionListener(e -> renderOrders());
        orderTable.getSelectionModel().addListSelectionListener(e -> {
            int r = orderTable.getSelectedRow(); if (r < 0 || e.getValueIsAdjusting()) return;
            orderDetail.setText(orderText(orders[orderPerm[r]])); orderDetail.setCaretPosition(0);
        });
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); bar.setOpaque(false);
        bar.add(label("Sort:", FB, TEXT)); bar.add(orderSort);
        bar.add(btn("Reorder selected", GREEN, e -> reorder()));
        JScrollPane dsp = scroll(orderDetail); dsp.setPreferredSize(new Dimension(0, 190));
        p.add(bar, BorderLayout.NORTH); p.add(scroll(orderTable)); p.add(dsp, BorderLayout.SOUTH);
        return p;
    }
    void refreshOrders() { if (user == null || orderModel == null) return; orders = OrderLog.load(user); renderOrders(); }
    void renderOrders() {
        int n = orders.length, k = orderSort.getSelectedIndex();
        if (k <= 1) { orderPerm = new int[n]; for (int i = 0; i < n; i++) orderPerm[i] = k == 0 ? n - 1 - i : i; }   // file is chronological
        else { long[] a = new long[n]; for (int i = 0; i < n; i++) a[i] = orders[i].amount; orderPerm = Sorter.byLong(a, k == 3); }
        orderModel.setRowCount(0); orderDetail.setText(n == 0 ? "No orders yet. Checkout from the Cart tab to create one." : "Select an order to see its items.");
        for (int i = 0; i < n; i++) { OrderRec o = orders[orderPerm[i]]; orderModel.addRow(new Object[]{o.id, o.date, o.items, o.amount}); }
    }
    String orderText(OrderRec o) {
        StringBuilder sb = new StringBuilder("Order " + o.id + "\nDate   : " + o.date + "\nItems  : " + o.items + "\nAmount : Rs " + o.amount + "\n\n");
        if (o.detail.length() == 0) return sb.append("(Item details were not recorded for this older order.)").toString();
        String[] parts = o.detail.split(";");
        for (String s : parts) {
            String[] x = s.split(":"); if (x.length < 2) continue;
            Product p = cat.byId(num(x[0], -1)); int q = num(x[1], 1);
            sb.append(String.format("%-6s %-46s x%-3d Rs %d%n", x[0], p == null ? "(product no longer listed)" : cut(p.name, 46), q, p == null ? 0 : (long) p.price * q));
        }
        return sb.toString();
    }
    void reorder() {
        int r = orderTable.getSelectedRow(); if (r < 0) { msg("Select an order first."); return; }
        OrderRec o = orders[orderPerm[r]];
        if (o.detail.length() == 0) { msg("This older order has no item details to reorder."); return; }
        int added = 0;
        for (String s : o.detail.split(";")) {
            String[] x = s.split(":"); if (x.length < 2) continue;
            int id = num(x[0], -1), q = num(x[1], 1);
            if (cat.byId(id) == null) continue;
            for (int i = 0; i < q; i++) cart.add(id);
            added += q;
        }
        refreshCart(); msg(added + " item(s) from " + o.id + " added to your cart.");
    }

    /* ---------- recommendations ---------- */
    JPanel recoPanel() {
        JTabbedPane rt = new JTabbedPane(); rt.setFont(F);
        rt.addTab("Best bundle (knapsack)", bundlePanel());
        rt.addTab("Aspect matching", aspectPanel());
        rt.addTab("Surprise picks", surprisePanel());
        rt.addTab("Similar products", similarPanel());
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(BG); p.add(rt); return p;
    }
    JPanel recoShell(JPanel inputs, JTextArea area) {
        JPanel p = new JPanel(new BorderLayout(0, 8)); p.setBackground(BG); p.setBorder(new EmptyBorder(8, 8, 8, 8));
        p.add(inputs, BorderLayout.NORTH); p.add(scroll(area)); return p;
    }

    IntList bundleIds = new IntList();
    JPanel bundlePanel() {
        JTextField q = new JTextField(24), b = new JTextField(8); style(q); style(b);
        JTextArea area = out(); JButton add = btn("Add bundle to cart", GREEN, null); add.setEnabled(false);
        JPanel in = card(new FlowLayout(FlowLayout.LEFT, 10, 0));
        in.add(label("Need:", FB, TEXT)); in.add(q); in.add(label("Budget Rs:", FB, TEXT)); in.add(b);
        in.add(btn("Find bundle", PRIMARY, e -> {
            try {
                int budget = num(b.getText(), 0);
                if (budget <= 0) { msg("Budget must be positive."); return; }
                bundleIds = new IntList(); add.setEnabled(false);
                engine.typeMode = 2;
                SearchEngine.Hit[] hits = engine.search(q.getText().trim(), category(), 0, budget, 20);
                engine.typeMode = 1;
                if (hits.length == 0) { area.setText("Nothing relevant inside that budget."); return; }
                int n = Math.min(hits.length, 14);
                int[] val = new int[n], wt = new int[n];
                for (int i = 0; i < n; i++) { val[i] = Math.max(1, hits[i].score); wt[i] = engine.pr(hits[i]).price; }
                IntList picked = GreedyRecommendation.knapsackPick(val, wt, budget);
                StringBuilder sb = new StringBuilder("BEST BUNDLE WITHIN Rs " + budget + "\n" + "-".repeat(70) + "\n");
                long total = 0; int rel = 0;
                for (int i = picked.size() - 1; i >= 0; i--) {
                    Product p = engine.pr(hits[picked.get(i)]);
                    sb.append(String.format("%-6d %-38s Rs %-8d relevance %d%n", p.id, cut(p.name, 38), p.price, val[picked.get(i)]));
                    total += p.price; rel += val[picked.get(i)]; bundleIds.add(p.id);
                }
                sb.append("-".repeat(70)).append("\nItems: ").append(picked.size()).append("   Total: Rs ").append(total)
                  .append("   Budget left: Rs ").append(budget - total)
                  .append("\nTotal relevance achieved: ").append(rel).append(" (maximum possible for this budget)");
                area.setText(sb.toString()); add.setEnabled(bundleIds.size() > 0);
            } catch (Exception ex) { engine.typeMode = 1; msg("Error: " + ex); }
        }));
        add.addActionListener(e -> { for (int i = 0; i < bundleIds.size(); i++) cart.add(bundleIds.get(i)); refreshCart(); msg("Bundle added. Cart has " + cart.totalItems() + " item(s)."); add.setEnabled(false); });
        in.add(add);
        return recoShell(in, area);
    }

    JPanel aspectPanel() {
        JTextField q = new JTextField(34); style(q); JTextArea area = out();
        JPanel in = card(new FlowLayout(FlowLayout.LEFT, 10, 0));
        in.add(label("Aspects (comma separated):", FB, TEXT)); in.add(q);
        in.add(btn("Match", PRIMARY, e -> {
            try {
                String s = q.getText();
                StrList aspects = new StrList(); int st = 0;
                for (int i = 0; i <= s.length(); i++)
                    if (i == s.length() || s.charAt(i) == ',') { String a = s.substring(st, i).trim(); if (a.length() > 0) aspects.add(a); st = i + 1; }
                if (aspects.size() == 0) { area.setText("No aspects given. Example: long battery, lightweight, good camera"); return; }
                engine.typeMode = 0;
                SearchEngine.Hit[] pool = engine.search(s, category(), 0, 0, 12);
                engine.typeMode = 1;
                if (pool.length == 0) { area.setText("No candidate products."); return; }
                int L = aspects.size(), R = pool.length;
                int[][] adj = new int[L][R];
                for (int i = 0; i < L; i++) {
                    StrList aw = Text.keywords(aspects.get(i));
                    for (int j = 0; j < R; j++) {
                        String blob = engine.pr(pool[j]).descLower; int hit = 0;
                        for (int k = 0; k < aw.size(); k++) if (PatternSearch.kmpSearch(blob, aw.get(k)) >= 0) hit++;
                        if (aw.size() > 0 && hit * 2 >= aw.size()) adj[i][j] = 1;
                    }
                }
                int[] match = NetworkFlow.bipartiteMatch(adj, L, R);
                StringBuilder sb = new StringBuilder("ASPECT -> PRODUCT ASSIGNMENT\n" + "-".repeat(80) + "\n"); int matched = 0;
                for (int i = 0; i < L; i++) {
                    if (match[i] >= 0) { Product p = engine.pr(pool[match[i]]); sb.append(String.format("%-28s -> [%d] %-34s Rs %d%n", cut(aspects.get(i), 28), p.id, cut(p.name, 34), p.price)); matched++; }
                    else sb.append(String.format("%-28s -> no distinct product covers this aspect%n", cut(aspects.get(i), 28)));
                }
                sb.append("\nMaximum matching size = ").append(matched).append(" of ").append(L).append(" aspects.");
                area.setText(sb.toString());
            } catch (Exception ex) { engine.typeMode = 1; msg("Error: " + ex); }
        }));
        return recoShell(in, area);
    }

    JPanel surprisePanel() {
        JTextArea area = out();
        JPanel in = card(new FlowLayout(FlowLayout.LEFT, 10, 0));
        in.add(btn("Surprise me (5 random picks)", PRIMARY, e -> {
            IntList stream = new IntList(); String c = category();
            for (int i = 0; i < cat.count; i++) if (c.equals("All Products") || cat.products[i].category.equals(c)) stream.add(i);
            int[] pick = ParallelSearch.reservoirSample(stream, 5, rand);
            StringBuilder sb = new StringBuilder("SURPRISE PICKS - " + c + "\n" + "-".repeat(70) + "\n");
            for (int p : pick) { Product pr = cat.products[p]; sb.append(String.format("%-6d %-40s Rs %-8d %s%n", pr.id, cut(pr.name, 40), pr.price, pr.type)); }
            area.setText(sb.toString());
        }));
        in.add(label("Uses the category selected at the top", F, MUTED));
        return recoShell(in, area);
    }

    JPanel similarPanel() {
        JTextField idf = new JTextField(8); style(idf); JTextArea area = out();
        JPanel in = card(new FlowLayout(FlowLayout.LEFT, 10, 0));
        in.add(label("Product ID you liked:", FB, TEXT)); in.add(idf);
        in.add(btn("Find similar", PRIMARY, e -> {
            Product base = cat.byId(num(idf.getText(), -1));
            if (base == null) { area.setText("No product with that ID."); return; }
            IntList pool = new IntList();
            for (int i = 0; i < cat.count; i++) { Product p = cat.products[i]; if (p.id == base.id || !p.category.equals(base.category)) continue; pool.add(i); }
            int limit = Math.min(pool.size(), 120);
            int[] sc = new int[limit], idx = new int[limit];
            for (int i = 0; i < limit; i++) {
                int p = pool.get(i);
                int lcs = SimilaritySearch.longestCommonSubstring(base.descLower, cat.products[p].descLower);
                int priceClose = Math.max(0, 20 - Math.abs(cat.products[p].price - base.price) * 20 / Math.max(1, base.price));
                int sameType = cat.products[p].type.equals(base.type) ? 150 : 0;
                int sameBrand = cat.products[p].brand.equals(base.brand) ? 15 : 0;
                sc[i] = lcs + priceClose + sameType + sameBrand; idx[i] = p;
            }
            ParallelSearch.randomizedQuickSort(sc, idx, 0, limit - 1, rand);
            StringBuilder sb = new StringBuilder("Similar to: " + base.name + "\n" + "-".repeat(70) + "\n");
            for (int i = 0; i < Math.min(5, limit); i++) { Product p = cat.products[idx[i]]; sb.append(String.format("%-6d %-40s Rs %-8d similarity %d%n", p.id, cut(p.name, 40), p.price, sc[i])); }
            area.setText(sb.toString());
        }));
        return recoShell(in, area);
    }

    /* ---------- cart ---------- */
    JPanel cartPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 10)); p.setBackground(BG);
        cartModel = new DefaultTableModel(new String[]{"ID", "Name", "Qty", "Price (Rs)", "Subtotal (Rs)"}, 0);
        cartTable = table(cartModel);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT)); bar.setOpaque(false);
        JTextField idf = new JTextField(6); style(idf);
        bar.add(label("Product ID:", FB, TEXT)); bar.add(idf);
        bar.add(btn("Add", GREEN, e -> {
            int id = num(idf.getText(), -1);
            if (cat.byId(id) == null) msg("No such product."); else { cart.add(id); refreshCart(); }
        }));
        bar.add(btn("Delete selected", RED, e -> {
            int r = cartTable.getSelectedRow();
            if (r < 0) { msg("Select a cart row first."); return; }
            cart.remove((Integer) cartTable.getValueAt(r, 0)); refreshCart();
        }));
        bar.add(btn("Checkout", PRIMARY, e -> checkout()));
        JPanel south = card(new GridLayout(2, 1, 0, 4));
        cartTotals.setFont(FB); cartRoute.setFont(F); cartRoute.setForeground(MUTED);
        south.add(cartTotals); south.add(cartRoute);
        p.add(bar, BorderLayout.NORTH); p.add(scroll(cartTable)); p.add(south, BorderLayout.SOUTH); return p;
    }

    void refreshCart() {
        if (cart == null) return;
        cartModel.setRowCount(0);
        long[] amounts = new long[cart.ids.size()];
        for (int i = 0; i < cart.ids.size(); i++) {
            Product p = cat.byId(cart.ids.get(i)); if (p == null) continue;
            long sub = (long) p.price * cart.qty.get(i); amounts[i] = sub;
            cartModel.addRow(new Object[]{p.id, p.name, cart.qty.get(i), p.price, sub});
        }
        String route = " ";
        if (cart.ids.size() == 0) { cartTotals.setText("Cart is empty."); }
        else {
            String extra = "";
            try {
                long[] par = ParallelSearch.parallelPrefixSum(amounts), seq = ParallelSearch.sequentialPrefixSum(amounts);
                boolean same = true; for (int i = 0; i < amounts.length; i++) if (par[i] != seq[i]) same = false;
                extra = "   |   Running total Rs " + (par.length > 0 ? par[par.length - 1] : 0) + (same ? " [verified]" : " [MISMATCH]")
                      + "   |   Parallel reduce Rs " + ParallelSearch.parallelReduce(amounts);
            } catch (Exception e) { }
            cartTotals.setText("Total items: " + cart.totalItems() + "   |   Total price: Rs " + cart.totalPrice() + extra);
            if (cart.ids.size() >= 2 && cart.ids.size() <= 10) route = "Shortest store pickup route: " + pickupRoute();
        }
        cartRoute.setText(route); updateCartLbl();
    }

    String pickupRoute() {
        int n = cart.ids.size(); int[] sec = new int[n];
        for (int i = 0; i < n; i++) {
            Product p = cat.byId(cart.ids.get(i)); sec[i] = 0;
            for (int c = 0; c < CATS.length; c++) if (p != null && CATS[c].equals(p.category)) sec[i] = c;
        }
        int[][] dist = new int[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) dist[i][j] = Math.abs(sec[i] - sec[j]) + 1;
        int[] order = GreedyRecommendation.tspOrder(dist);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < order.length; i++) {
            Product p = cat.byId(cart.ids.get(order[i]));
            sb.append(p == null ? "?" : p.category).append(i < order.length - 1 ? " -> " : "");
        }
        return sb.toString();
    }

    void checkout() {
        if (cart.ids.size() == 0) { msg("Cart is empty."); return; }
        long candidate = 100003 + rand.next(900000); int tries = 0;
        while (!ParallelSearch.millerRabin(candidate, 8, rand) && tries < 20000) { candidate++; tries++; }
        int items = cart.totalItems(); long amt = cart.totalPrice();
        StringBuilder det = new StringBuilder();
        for (int i = 0; i < cart.ids.size(); i++) det.append(i > 0 ? ";" : "").append(cart.ids.get(i)).append(':').append(cart.qty.get(i));
        OrderLog.append("ORD-" + candidate, user, java.time.LocalDateTime.now().toString().substring(0, 16).replace('T', ' '), det.toString());
        try {
            PrintWriter pw = new PrintWriter(new FileWriter("data/orders.txt", true));
            pw.println("ORD-" + candidate + "," + user + "," + items + "," + amt); pw.close();
        } catch (Exception e) { }
        cart.ids.clear(); cart.qty.clear(); cart.save(); refreshCart(); refreshOrders();
        msg("ORDER PLACED\n\nOrder ID : ORD-" + candidate + "  (prime, verified)\nItems    : " + items + "\nAmount   : Rs " + amt
            + "\nCustomer : " + user + "\n\nOrder saved to data/orders.txt. Cart cleared.");
    }
}
