/* Text - manual tokenizer and stop-word handling. */
class Text {
    static String[] STOP = {"a","an","the","is","are","for","with","and","of","to","i","want","need",
            "in","on","my","me","that","this","it","be","can","you","please","looking","under","below",
            "less","than","around","about","rs","rupees","budget","good","best","some","any","which","one","friendly","cheap","affordable","economical","inexpensive",
            "premium","luxury","expensive","costly","cost","price","priced","buy","get","show","find","give","like",
            "have","would","could","should","from","also","very","really","more","most","latest"};

    static StrList tokenize(String s) {
        StrList out = new StrList();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = Character.toLowerCase(s.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) cur.append(c);
            else { if (cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); } }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    static boolean isStop(String w) {
        if (w.length() < 3) return true;
        for (String s : STOP) if (s.equals(w)) return true;
        return false;
    }

    /* keywords = tokens minus stop words */
    static StrList keywords(String q) {
        StrList all = Text.tokenize(q);
        StrList out = new StrList();
        for (int i = 0; i < all.size(); i++) {
            String w = all.get(i);
            if (!isStop(w) && !out.contains(w)) out.add(w);
        }
        return out;
    }

    /* pull a budget out of free text: "under 20000", "below 5000 rs" */
    static int extractBudget(String q) {
        StrList t = Text.tokenize(q);
        int budget = -1;
        for (int i = 0; i < t.size(); i++) {
            String w = t.get(i);
            boolean num = w.length() > 2;
            for (int c = 0; c < w.length() && num; c++) if (w.charAt(c) < '0' || w.charAt(c) > '9') num = false;
            if (num) {
                try { int v = Integer.parseInt(w); if (v > budget) budget = v; } catch (Exception e) {}
            }
        }
        return budget;
    }
}
