package com.lucerne.dao;

import com.lucerne.config.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public final class ProductDAO {
    public QueryDAO.QueryResult search(String keyword,Integer categoryId,String stockStatus,boolean includeInactive,boolean includeCost,int limit,int offset) throws SQLException {
        StringBuilder where=new StringBuilder(" WHERE 1=1");List<Object> p=new ArrayList<>();
        if(keyword!=null&&!keyword.isBlank()){where.append(" AND (p.Name LIKE ? OR p.SKU LIKE ? OR p.Barcode LIKE ?)");String v="%"+keyword.trim()+"%";p.add(v);p.add(v);p.add(v);}
        if(categoryId!=null){where.append(" AND p.CategoryID=?");p.add(categoryId);}
        if(!includeInactive)where.append(" AND p.IsActive=1");
        if(stockStatus!=null&&!stockStatus.equals("All")){
            switch(stockStatus){case "In stock"->where.append(" AND COALESCE(stock.TotalStock,0)>p.ReorderLevel");case "Low stock"->where.append(" AND COALESCE(stock.TotalStock,0)>0 AND COALESCE(stock.TotalStock,0)<=p.ReorderLevel");case "Out of stock"->where.append(" AND COALESCE(stock.TotalStock,0)=0");default->{}}
        }
        String financialColumns = includeCost
                ? "p.SellingPrice AS Price,p.CostPrice AS Cost,(p.SellingPrice-p.CostPrice) AS Margin," +
                  "ROUND((p.SellingPrice-p.CostPrice)/NULLIF(p.SellingPrice,0)*100,1) AS `Margin %`,"
                : "p.SellingPrice AS Price,";
        String base="""
                SELECT p.ProductID AS `Product ID`,p.ImagePath AS Image,p.SKU,p.Barcode,p.Name AS Product,
                       c.CategoryName AS Category,COALESCE(sc.SubcategoryName,'—') AS Subcategory,
                """ + financialColumns + """
                       COALESCE(stock.TotalStock,0) AS Stock,p.ReorderLevel AS `Reorder Level`,p.IsActive AS Active,
                       p.Description,p.Material,p.CareInstructions AS `Care Instructions`,p.UpdatedAt AS `Last Updated`
                FROM products p JOIN categories c ON c.CategoryID=p.CategoryID
                LEFT JOIN subcategories sc ON sc.SubcategoryID=p.SubcategoryID
                LEFT JOIN (
                    SELECT pv.ProductID,
                           SUM(COALESCE(bs.BranchStock,0)+COALESCE(ws.WarehouseStock,0)) AS TotalStock
                    FROM product_variants pv
                    LEFT JOIN (SELECT VariantID,SUM(Quantity) AS BranchStock FROM branch_inventory GROUP BY VariantID) bs
                           ON bs.VariantID=pv.VariantID
                    LEFT JOIN (SELECT VariantID,SUM(Quantity) AS WarehouseStock FROM warehouse_inventory GROUP BY VariantID) ws
                           ON ws.VariantID=pv.VariantID
                    GROUP BY pv.ProductID
                ) stock ON stock.ProductID=p.ProductID
                """+where;
        String count="SELECT COUNT(*) FROM ("+base+") counted_products";
        return new QueryDAO().query(base+" ORDER BY p.UpdatedAt DESC,p.Name",count,p,limit,offset);
    }

    public List<QueryDAO.Option> categories() throws SQLException {return new QueryDAO().options("SELECT CategoryID,CategoryName FROM categories WHERE IsActive=1 ORDER BY CategoryName",List.of());}
    public List<QueryDAO.Option> subcategories(int categoryId) throws SQLException {return new QueryDAO().options("SELECT SubcategoryID,SubcategoryName FROM subcategories WHERE CategoryID=? AND IsActive=1 ORDER BY SubcategoryName",List.of(categoryId));}
    public int create(ProductForm f,int userId) throws SQLException {
        String sql="""
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,IsActive,CreatedBy,UpdatedBy,CreatedAt,UpdatedAt)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,1,?,?,NOW(),NOW())""";
        try(Connection c=DatabaseConnection.open();PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
            bind(s,f,userId);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){r.next();return r.getInt(1);}
        }
    }
    public void update(int productId,ProductForm f,int userId) throws SQLException {
        String sql="""
UPDATE products SET SKU=?,Barcode=?,Name=?,CategoryID=?,SubcategoryID=?,SellingPrice=?,CostPrice=?,Description=?,Material=?,CareInstructions=?,ImagePath=?,ReorderLevel=?,UpdatedBy=?,UpdatedAt=NOW() WHERE ProductID=?""";
        try(Connection c=DatabaseConnection.open()){c.setAutoCommit(false);try{
            BigDecimal oldPrice=null;try(PreparedStatement old=c.prepareStatement("SELECT SellingPrice FROM products WHERE ProductID=? FOR UPDATE")){old.setInt(1,productId);try(ResultSet r=old.executeQuery()){if(r.next())oldPrice=r.getBigDecimal(1);}}
            try(PreparedStatement s=c.prepareStatement(sql)){int i=1;s.setString(i++,f.sku());s.setString(i++,f.barcode());s.setString(i++,f.name());s.setInt(i++,f.categoryId());if(f.subcategoryId()==null)s.setNull(i++,Types.INTEGER);else s.setInt(i++,f.subcategoryId());s.setBigDecimal(i++,f.price());s.setBigDecimal(i++,f.cost());s.setString(i++,f.description());s.setString(i++,f.material());s.setString(i++,f.care());s.setString(i++,f.imagePath());s.setInt(i++,f.reorderLevel());s.setInt(i++,userId);s.setInt(i,productId);s.executeUpdate();}
            if(oldPrice!=null&&oldPrice.compareTo(f.price())!=0){try(PreparedStatement h=c.prepareStatement("INSERT INTO price_history(ProductID,OldPrice,NewPrice,ChangedBy,ChangedAt) VALUES(?,?,?,?,NOW())")){h.setInt(1,productId);h.setBigDecimal(2,oldPrice);h.setBigDecimal(3,f.price());h.setInt(4,userId);h.executeUpdate();}}
            AuthDAO.insertAudit(c,userId,null,"PRODUCT_UPDATE","PRODUCT",productId,"Product details updated",true,null);c.commit();
        }catch(SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}
    }
    public void setActive(int productId,boolean active,int userId)throws SQLException{try(Connection c=DatabaseConnection.open();PreparedStatement s=c.prepareStatement("UPDATE products SET IsActive=?,UpdatedBy=?,UpdatedAt=NOW() WHERE ProductID=?")){s.setBoolean(1,active);s.setInt(2,userId);s.setInt(3,productId);s.executeUpdate();}}
    private void bind(PreparedStatement s,ProductForm f,int userId)throws SQLException{int i=1;s.setString(i++,f.sku());s.setString(i++,f.barcode());s.setString(i++,f.name());s.setInt(i++,f.categoryId());if(f.subcategoryId()==null)s.setNull(i++,Types.INTEGER);else s.setInt(i++,f.subcategoryId());s.setBigDecimal(i++,f.price());s.setBigDecimal(i++,f.cost());s.setString(i++,f.description());s.setString(i++,f.material());s.setString(i++,f.care());s.setString(i++,f.imagePath());s.setInt(i++,f.reorderLevel());s.setInt(i++,userId);s.setInt(i,userId);}
    public record ProductForm(String sku,String barcode,String name,int categoryId,Integer subcategoryId,BigDecimal price,BigDecimal cost,String description,String material,String care,String imagePath,int reorderLevel){}
}
