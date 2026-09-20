/* Product - one row of products.csv */
class Product {
    int id;
    String name, brand, category, type, desc;
    int price;
    String descLower, nameLower;

    Product(int id, String name, String brand, String category, String type, int price, String desc) {
        this.id = id; this.name = name; this.brand = brand; this.category = category;
        this.type = type; this.price = price; this.desc = desc;
        this.descLower = desc.toLowerCase();
        this.nameLower = name.toLowerCase();
    }
}
