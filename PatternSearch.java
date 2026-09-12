public class PatternSearch {

    // KMP Pattern Matching
    public static boolean contains(String text, String pattern) {

        if (pattern.length() == 0) {
            return true;
        }

        int[] lps = buildLPS(pattern);

        int i = 0;
        int j = 0;

        while (i < text.length()) {

            if (Character.toLowerCase(text.charAt(i))
                    == Character.toLowerCase(pattern.charAt(j))) {

                i++;
                j++;

                if (j == pattern.length()) {
                    return true;
                }

            } else {

                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }

        return false;
    }

    // Build Longest Prefix Suffix array
    private static int[] buildLPS(String pattern) {

        int[] lps = new int[pattern.length()];

        int length = 0;
        int i = 1;

        while (i < pattern.length()) {

            if (Character.toLowerCase(pattern.charAt(i))
                    == Character.toLowerCase(pattern.charAt(length))) {

                length++;
                lps[i] = length;
                i++;

            } else {

                if (length != 0) {
                    length = lps[length - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }

        return lps;
    }
}