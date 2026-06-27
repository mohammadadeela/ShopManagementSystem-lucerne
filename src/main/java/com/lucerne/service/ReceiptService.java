package com.lucerne.service;

import com.lucerne.dao.SalesDAO;

import java.math.BigDecimal;
import java.util.Map;

/** Produces a UTF-8 printable text receipt from stored sale data. */
public final class ReceiptService {
    private final SalesDAO salesDAO = new SalesDAO();
    private final SettingsService settings = new SettingsService();

    public String render(long saleId) throws Exception {
        SalesDAO.SaleDetails details = salesDAO.details(saleId);
        Map<String,Object> h = details.header();
        String boutique = settings.get("boutique_name", "Lucerne Boutique");
        String phone = settings.get("company_phone", "");
        String address = settings.get("company_address", "");
        String currency = settings.get("currency", "USD");
        String footer = settings.get("receipt_footer", "Thank you for shopping with us");
        StringBuilder out = new StringBuilder();
        line(out, 48, '=');
        center(out, boutique, 48);
        if (!address.isBlank()) center(out, address, 48);
        if (!phone.isBlank()) center(out, phone, 48);
        line(out, 48, '-');
        out.append("Receipt: ").append(h.get("ReceiptNumber")).append('\n');
        out.append("Date:    ").append(h.get("SaleDate")).append('\n');
        out.append("Branch:  ").append(h.get("Branch")).append('\n');
        out.append("Cashier: ").append(h.get("Cashier")).append('\n');
        out.append("Customer:").append(' ').append(h.get("Customer")).append('\n');
        line(out, 48, '-');
        out.append(String.format("%-23s %4s %8s %9s%n", "Item", "Qty", "Price", "Total"));
        for (Map<String,Object> item : details.items()) {
            String product = String.valueOf(item.get("Product"));
            if (product.length() > 23) product = product.substring(0, 23);
            out.append(String.format("%-23s %4s %8.2f %9.2f%n", product, item.get("Quantity"),
                    decimal(item.get("Unit Price")), decimal(item.get("Line Total"))));
            out.append("  ").append(item.get("Color")).append(" / ").append(item.get("Size")).append('\n');
        }
        line(out, 48, '-');
        money(out, "Subtotal", h.get("GrossAmount"), currency);
        money(out, "Discount", h.get("DiscountAmount"), currency);
        money(out, "Total", h.get("NetAmount"), currency);
        money(out, "Paid", h.get("PaidAmount"), currency);
        money(out, "Change", h.get("ChangeAmount"), currency);
        out.append("Payment: ").append(h.get("PaymentMethod")).append('\n');
        out.append("Status:  ").append(h.get("Status")).append('\n');
        line(out, 48, '=');
        center(out, footer, 48);
        return out.toString();
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        return new BigDecimal(value.toString());
    }
    private static void money(StringBuilder out, String label, Object value, String currency) {
        out.append(String.format("%-30s %12.2f %s%n", label, decimal(value), currency));
    }
    private static void line(StringBuilder out, int width, char character) { out.append(String.valueOf(character).repeat(width)).append('\n'); }
    private static void center(StringBuilder out, String value, int width) {
        String text = value == null ? "" : value;
        int padding = Math.max(0, (width - text.length()) / 2);
        out.append(" ".repeat(padding)).append(text).append('\n');
    }
}
