public class SimilaritySearch {

    // Calculate similarity percentage
    public static double calculateSimilarity(
            String query,
            String productText) {

        query = query.toLowerCase();
        productText = productText.toLowerCase();

        String[] queryWords = query.split("\\s+");

        int matchedWords = 0;

        for (String queryWord : queryWords) {

            if (queryWord.length() == 0) {
                continue;
            }

            String[] productWords = productText.split("\\s+");

            for (String productWord : productWords) {

                productWord =
                        productWord.replaceAll("[^a-zA-Z0-9]", "");

                if (queryWord.equals(productWord)) {
                    matchedWords++;
                    break;
                }
            }
        }

        if (queryWords.length == 0) {
            return 0;
        }

        return ((double) matchedWords / queryWords.length) * 100;
    }
}