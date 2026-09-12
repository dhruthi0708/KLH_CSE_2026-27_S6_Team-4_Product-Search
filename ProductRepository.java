import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ProductRepository {

    private Product[] products;
    private int count;

    public ProductRepository(String filePath) {
        products = new Product[1000];
        count = 0;
        loadProducts(filePath);
    }

    private void loadProducts(String filePath) {

        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));

            // Skip header
            br.readLine();

            String line;

            while ((line = br.readLine()) != null) {

                String[] data = line.split(",", -1);

                if (data.length >= 7) {

                    int id = Integer.parseInt(data[0]);
                    String name = data[1];
                    String brand = data[2];
                    String category = data[3];
                    String type = data[4];
                    double price = Double.parseDouble(data[5]);
                    String description = data[6];

                    products[count] = new Product(
                            id,
                            name,
                            brand,
                            category,
                            type,
                            price,
                            description
                    );

                    count++;
                }
            }

            br.close();

            System.out.println("Repository loaded successfully.");
            System.out.println("Total products: " + count);

        } catch (IOException e) {
            System.out.println("Error reading product file.");
        }
    }

    public Product[] getProducts() {
        return products;
    }

    public int getCount() {
        return count;
    }
}