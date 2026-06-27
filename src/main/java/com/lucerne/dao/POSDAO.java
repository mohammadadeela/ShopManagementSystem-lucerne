package com.lucerne.dao;

import com.lucerne.config.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public final class POSDAO {
    public List<VariantRow> search(int branchId,String keyword,Integer categoryId) throws SQLException {
        StringBuilder sql=new StringBuilder("""
                SELECT pv.VariantID,p.ProductID,p.Name,p.SKU,p.Barcode,p.ImagePath,c.CategoryName,
                       sz.SizeValue,co.ColorName,p.SellingPrice,bi.Quantity
                FROM branch_inventory bi JOIN product_variants pv ON pv.VariantID=bi.VariantID
                JOIN products p ON p.ProductID=pv.ProductID JOIN categories c ON c.CategoryID=p.CategoryID
                JOIN sizes sz ON sz.SizeID=pv.SizeID JOIN colors co ON co.ColorID=pv.ColorID
                WHERE bi.BranchID=? AND p.IsActive=1 AND pv.IsActive=1 AND bi.Quantity>0
                """);
        List<Object> params=new ArrayList<>();params.add(branchId);
        if(keyword!=null&&!keyword.isBlank()){sql.append(" AND (p.Name LIKE ? OR p.SKU LIKE ? OR p.Barcode LIKE ?)");String value="%"+keyword.trim()+"%";params.add(value);params.add(value);params.add(value);}
        if(categoryId!=null){sql.append(" AND p.CategoryID=?");params.add(categoryId);}
        sql.append(" ORDER BY p.Name,co.ColorName,sz.SortOrder LIMIT 200");
        List<VariantRow> result=new ArrayList<>();
        try(Connection c=DatabaseConnection.open();PreparedStatement s=c.prepareStatement(sql.toString())){QueryDAO.bind(s,params);try(ResultSet r=s.executeQuery()){while(r.next())result.add(new VariantRow(r.getInt("VariantID"),r.getInt("ProductID"),r.getString("Name"),r.getString("SKU"),r.getString("Barcode"),r.getString("ImagePath"),r.getString("CategoryName"),r.getString("SizeValue"),r.getString("ColorName"),r.getBigDecimal("SellingPrice"),r.getInt("Quantity")));}}return result;
    }
    public List<QueryDAO.Option> customers() throws SQLException{return new QueryDAO().options("SELECT CustomerID,CONCAT(FullName,' — ',Phone) FROM customers WHERE IsActive=1 ORDER BY FullName LIMIT 500",List.of());}
    public List<QueryDAO.Option> categories() throws SQLException{return new QueryDAO().options("SELECT CategoryID,CategoryName FROM categories WHERE IsActive=1 ORDER BY CategoryName",List.of());}
    public DiscountResult validateDiscount(String code,BigDecimal subtotal)throws SQLException{if(code==null||code.isBlank())return new DiscountResult(null,BigDecimal.ZERO,"No discount");try(Connection c=DatabaseConnection.open();PreparedStatement s=c.prepareStatement("SELECT DiscountID,Percentage,FixedAmount,MinimumPurchase,MaximumDiscount FROM discounts WHERE Code=? AND IsActive=1 AND CURDATE() BETWEEN StartDate AND EndDate")){s.setString(1,code.trim().toUpperCase());try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Invalid discount");BigDecimal minimum=r.getBigDecimal("MinimumPurchase");if(minimum!=null&&subtotal.compareTo(minimum)<0)throw new SQLException("Minimum purchase not met");BigDecimal amount=r.getBigDecimal("FixedAmount");if(amount==null||amount.signum()==0)amount=subtotal.multiply(r.getBigDecimal("Percentage")).divide(BigDecimal.valueOf(100),2,java.math.RoundingMode.HALF_UP);BigDecimal max=r.getBigDecimal("MaximumDiscount");if(max!=null&&amount.compareTo(max)>0)amount=max;if(amount.compareTo(subtotal)>0)amount=subtotal;return new DiscountResult(r.getInt("DiscountID"),amount,"Discount applied");}}}
    public record VariantRow(int variantId,int productId,String name,String sku,String barcode,String imagePath,String category,String size,String color,BigDecimal price,int stock){}
    public record DiscountResult(Integer discountId,BigDecimal amount,String message){}
}
