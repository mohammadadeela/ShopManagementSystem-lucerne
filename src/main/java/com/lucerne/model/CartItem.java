package com.lucerne.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import java.math.BigDecimal;

public final class CartItem {
    private final int variantId;
    private final int productId;
    private final String productName;
    private final String size;
    private final String color;
    private final BigDecimal unitPrice;
    private final int availableStock;
    private final IntegerProperty quantity = new SimpleIntegerProperty(1);

    public CartItem(int variantId, int productId, String productName, String size, String color,
                    BigDecimal unitPrice, int availableStock) {
        this.variantId = variantId;
        this.productId = productId;
        this.productName = productName;
        this.size = size;
        this.color = color;
        this.unitPrice = unitPrice;
        this.availableStock = availableStock;
    }

    public int variantId() { return variantId; }
    public int productId() { return productId; }
    public String productName() { return productName; }
    public String size() { return size; }
    public String color() { return color; }
    public BigDecimal unitPrice() { return unitPrice; }
    public int availableStock() { return availableStock; }
    public IntegerProperty quantityProperty() { return quantity; }
    public int quantity() { return quantity.get(); }
    public void setQuantity(int value) { quantity.set(Math.max(1, Math.min(value, availableStock))); }
    public BigDecimal lineTotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity())); }
}
