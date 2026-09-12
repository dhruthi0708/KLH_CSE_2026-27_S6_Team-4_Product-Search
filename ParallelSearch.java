public class ParallelSearch {

    // CO6: Parallel Product Search
    public static void search(
            Product[] products,
            int count,
            String query) {

        System.out.println();
        System.out.println("==============================================");
        System.out.println("        CO6 - PARALLEL PRODUCT SEARCH");
        System.out.println("==============================================");

        Thread t1 = new Thread(() -> {
            searchRange(products, 0, count / 2, query, "Thread 1");
        });

        Thread t2 = new Thread(() -> {
            searchRange(products, count / 2, count, query, "Thread 2");
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            System.out.println("Search interrupted.");
        }

        System.out.println();
        System.out.println("Parallel search completed.");
    }

    // User-defined function
    private static void searchRange(
            Product[] products,
            int start,
            int end,
            String query,
            String threadName) {

        for (int i = start; i < end; i++) {

            Product product = products[i];

            if (product.getName().toLowerCase()
                    .contains(query.toLowerCase())
                    || product.getCategory().toLowerCase()
                    .contains(query.toLowerCase())
                    || product.getBrand().toLowerCase()
                    .contains(query.toLowerCase())) {

                System.out.println(
                        threadName + " found: "
                        + product.getName()
                );
            }
        }
    }
}