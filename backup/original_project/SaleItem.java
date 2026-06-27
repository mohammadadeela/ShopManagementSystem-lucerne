public class SaleItem {
    private int saleItemId;
    private int saleId;
    private int productId;
    private int sizeId;
    private int quantity;
    private double unitPrice;

    public SaleItem(int saleItemId, int saleId, int productId,
                    int sizeId, int quantity, double unitPrice) {
        this.saleItemId = saleItemId;
        this.saleId = saleId;
        this.productId = productId;
        this.sizeId = sizeId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getSaleItemId() { return saleItemId; }
    public int getSaleId() { return saleId; }
    public int getProductId() { return productId; }
    public int getSizeId() { return sizeId; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
}
