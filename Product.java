public class Product {

    private int id;
    private String name;
    private String brand;
    private String category;
    private String type;
    private double price;
    private String description;

    public Product(int id, String name, String brand, String category,
                   String type, double price, String description) {

        this.id = id;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.type = type;
        this.price = price;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public String getCategory() {
        return category;
    }

    public String getType() {
        return type;
    }

    public double getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }
}