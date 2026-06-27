package com.lucerne.model;

import java.math.BigDecimal;

public record Product(
        int productId, String sku, String barcode, String name,
        String category, String subcategory, BigDecimal sellingPrice,
        BigDecimal costPrice, String imagePath, boolean active,
        int totalStock, int reorderLevel
) {
    public BigDecimal margin() { return sellingPrice.subtract(costPrice); }
}
