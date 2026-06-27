public class Inventory {
    private int inventoryId;
    private int locationId;
    private int productId;
    private int sizeId;
    private int quantity;

    public Inventory(int inventoryId, int locationId, int productId, int sizeId, int quantity) {
        this.inventoryId = inventoryId;
        this.locationId = locationId;
        this.productId = productId;
        this.sizeId = sizeId;
        this.quantity = quantity;
    }

    public int getInventoryId() { return inventoryId; }
    public int getLocationId() { return locationId; }
    public int getProductId() { return productId; }
    public int getSizeId() { return sizeId; }
    public int getQuantity() { return quantity; }
}
