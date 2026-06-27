public class Product {
    private int productId;
    private String name;
    private String category;
    private double price;
    private double costPrice;

    public Product(int productId, String name, String category, double price, double costPrice) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.costPrice = costPrice;
    }

    public int getProductId() { return productId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public double getCostPrice() { return costPrice; }
    public String toString() { return name; }
}
