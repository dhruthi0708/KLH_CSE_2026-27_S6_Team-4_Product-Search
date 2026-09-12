public class GreedyRecommendation {

    // CO5: Greedy Product Selection
    public static void recommend(Product[] products, int count, double budget) {

        System.out.println();
        System.out.println("==============================================");
        System.out.println("        CO5 - GREEDY RECOMMENDATION");
        System.out.println("==============================================");

        System.out.println("Budget: ₹" + budget);
        System.out.println();
        System.out.println("Selected Products:");

        double remainingBudget = budget;
        int selected = 0;

        // Greedily select affordable products
        for (int i = 0; i < count; i++) {

            Product product = products[i];

            if (product.getPrice() <= remainingBudget) {

                System.out.println(
                        product.getName()
                        + " - ₹"
                        + product.getPrice()
                );

                remainingBudget -= product.getPrice();
                selected++;
            }

            if (selected == 5) {
                break;
            }
        }

        System.out.println();
        System.out.println("Remaining Budget: ₹" + remainingBudget);
        System.out.println("Products Selected: " + selected);
    }
}