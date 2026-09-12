import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        ProductRepository repository =
                new ProductRepository("data/products.csv");

        Product[] products = repository.getProducts();

        Scanner scanner = new Scanner(System.in);

        System.out.println();
        System.out.println("==============================================");
        System.out.println("          PRODUCT SEARCH SYSTEM");
        System.out.println("==============================================");

        System.out.print("Enter your search: ");
        String query = scanner.nextLine().trim();

        System.out.println();
        System.out.println("Search: " + query);
        System.out.println();

        int found = 0;

        // 1. KMP Pattern Search

        for (int i = 0; i < repository.getCount(); i++) {

            Product product = products[i];

            boolean match =
                    PatternSearch.contains(product.getName(), query)
                    || PatternSearch.contains(product.getDescription(), query)
                    || PatternSearch.contains(product.getCategory(), query)
                    || PatternSearch.contains(product.getBrand(), query)
                    || PatternSearch.contains(product.getType(), query);

            if (match) {

                if (found == 0) {
                    System.out.println("Matching Products:");
                    System.out.println();
                }

                displayProduct(product);
                found++;
            }
        }

        // 2. Fuzzy Search

        if (found == 0) {

            String suggestion = findSuggestion(
                    products,
                    repository.getCount(),
                    query
            );

            if (suggestion != null) {

                System.out.println("Did you mean: " + suggestion + "?");
                System.out.println();
                System.out.println("Related Products:");
                System.out.println();

                for (int i = 0; i < repository.getCount(); i++) {

                    Product product = products[i];

                    if (containsWord(product.getName(), suggestion)
                            || containsWord(product.getCategory(), suggestion)
                            || containsWord(product.getType(), suggestion)
                            || containsWord(product.getDescription(), suggestion)) {

                        displayProduct(product);
                        found++;
                    }
                }
            }
        }

        // 3. Similarity Search

        System.out.println();

        System.out.println("==============================================");
        System.out.println("          SIMILARITY RESULTS");
        System.out.println("==============================================");

        for (int i = 0; i < repository.getCount(); i++) {

            Product product = products[i];

            String productText =
                    product.getName() + " "
                    + product.getBrand() + " "
                    + product.getCategory() + " "
                    + product.getType() + " "
                    + product.getDescription();

            double score =
                    SimilaritySearch.calculateSimilarity(
                            query,
                            productText
                    );

            if (score > 0) {

                System.out.println(
                        product.getName() + "  →  "
                        + String.format("%.0f", score)
                        + "% match"
                );
            }
        }

        if (found == 0) {
            System.out.println();
            System.out.println("No matching products found.");
        }

        System.out.println();
        System.out.println("==============================================");

        scanner.close();
    }

    // Display product details
    private static void displayProduct(Product product) {

        System.out.println("----------------------------------------------");
        System.out.println(product.getName());
        System.out.println("Brand      : " + product.getBrand());
        System.out.println("Category   : " + product.getCategory());
        System.out.println("Type       : " + product.getType());
        System.out.println("Price      : ₹" + product.getPrice());
        System.out.println("Description: " + product.getDescription());
        System.out.println("----------------------------------------------");
        System.out.println();
    }

    // Find closest spelling suggestion
    private static String findSuggestion(
            Product[] products,
            int count,
            String query) {

        String bestMatch = null;
        int bestDistance = 3;

        for (int i = 0; i < count; i++) {

            String[] words = products[i].getName().split("\\s+");

            for (String word : words) {

                word = word.replaceAll("[^a-zA-Z0-9]", "");

                int distance =
                        FuzzySearch.editDistance(query, word);

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestMatch = word;
                }
            }
        }

        return bestMatch;
    }

    // Check whether a word exists in text
    private static boolean containsWord(
            String text,
            String word) {

        String[] words = text.split("\\s+");

        for (String current : words) {

            current = current.replaceAll("[^a-zA-Z0-9]", "");

            if (current.equalsIgnoreCase(word)) {
                return true;
            }
        }

        return false;
    }
}