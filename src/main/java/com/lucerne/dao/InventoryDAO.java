package com.lucerne.dao;

import com.lucerne.app.AppSession;
import com.lucerne.config.DatabaseConnection;
import com.lucerne.service.AuthorizationService;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Server-side inventory filtering and transactional stock operations. */
public final class InventoryDAO {
    private final QueryDAO queryDAO=new QueryDAO();

    public QueryDAO.QueryResult search(InventoryFilter filter,int limit,int offset)throws SQLException{
        StringBuilder where=new StringBuilder(" WHERE 1=1");List<Object> p=new ArrayList<>();
        if(filter.keyword()!=null&&!filter.keyword().isBlank()){where.append(" AND (ProductName LIKE ? OR SKU LIKE ? OR ColorName LIKE ? OR SizeValue LIKE ?)");String v="%"+filter.keyword().trim()+"%";p.add(v);p.add(v);p.add(v);p.add(v);}
        if(filter.locationType()!=null){where.append(" AND LocationType=?");p.add(filter.locationType());}
        if(filter.locationId()!=null){if("BRANCH".equals(filter.locationType()))where.append(" AND BranchID=?");else if("WAREHOUSE".equals(filter.locationType()))where.append(" AND WarehouseID=?");p.add(filter.locationId());}
        if(filter.category()!=null){where.append(" AND CategoryName=?");p.add(filter.category());}
        if(filter.stockStatus()!=null){where.append(" AND StockStatus=?");p.add(filter.stockStatus());}
        if(filter.minimumQuantity()!=null){where.append(" AND Quantity>=?");p.add(filter.minimumQuantity());}
        if(filter.maximumQuantity()!=null){where.append(" AND Quantity<=?");p.add(filter.maximumQuantity());}
        if(filter.updatedFrom()!=null){where.append(" AND DATE(LastUpdated)>=?");p.add(filter.updatedFrom());}
        if(filter.updatedTo()!=null){where.append(" AND DATE(LastUpdated)<=?");p.add(filter.updatedTo());}
        applySessionScope(where,p);
        String base="""
                SELECT LocationType AS `Location Type`,LocationName AS Location,ProductID AS `Product ID`,VariantID AS `Variant ID`,
                       SKU,ProductName AS Product,CategoryName AS Category,SizeValue AS Size,ColorName AS Color,
                       Quantity,ReorderLevel AS `Reorder Level`,StockStatus AS `Stock Status`,CostValue AS `Cost Value`,
                       RetailValue AS `Retail Value`,LastUpdated AS `Last Updated`,BranchID AS `Branch ID`,WarehouseID AS `Warehouse ID`
                FROM v_current_inventory
                """+where;
        return queryDAO.query(base+" ORDER BY Product,Location,Color,Size","SELECT COUNT(*) FROM ("+base+") counted",p,limit,offset);
    }

    private void applySessionScope(StringBuilder where,List<Object> p){
        switch(AppSession.current().role()){
            case MANAGER,CASHIER->{if(AppSession.current().branchId()!=null){where.append(" AND LocationType='BRANCH' AND BranchID=?");p.add(AppSession.current().branchId());}}
            case WAREHOUSE->{if(AppSession.current().warehouseId()!=null){where.append(" AND LocationType='WAREHOUSE' AND WarehouseID=?");p.add(AppSession.current().warehouseId());}}
            default->{}
        }
    }

    public Summary summary(InventoryFilter filter)throws SQLException{
        StringBuilder where=new StringBuilder(" WHERE 1=1");List<Object> p=new ArrayList<>();
        if(filter.locationType()!=null){where.append(" AND LocationType=?");p.add(filter.locationType());}
        if(filter.locationId()!=null){if("BRANCH".equals(filter.locationType()))where.append(" AND BranchID=?");else where.append(" AND WarehouseID=?");p.add(filter.locationId());}
        if(filter.category()!=null){where.append(" AND CategoryName=?");p.add(filter.category());}
        if(filter.stockStatus()!=null){where.append(" AND StockStatus=?");p.add(filter.stockStatus());}
        applySessionScope(where,p);
        String sql="SELECT COALESCE(SUM(Quantity),0),COALESCE(SUM(CostValue),0),COALESCE(SUM(RetailValue),0),SUM(StockStatus='LOW_STOCK'),SUM(StockStatus='OUT_OF_STOCK'),SUM(StockStatus='OVERSTOCK') FROM v_current_inventory"+where;
        try(Connection c=DatabaseConnection.open();PreparedStatement s=c.prepareStatement(sql)){QueryDAO.bind(s,p);try(ResultSet r=s.executeQuery()){r.next();return new Summary(r.getLong(1),r.getBigDecimal(2),r.getBigDecimal(3),r.getInt(4),r.getInt(5),r.getInt(6));}}
    }
    public List<Map<String,Object>> categorySummary(InventoryFilter filter)throws SQLException{
        StringBuilder where=new StringBuilder(" WHERE 1=1");List<Object> p=new ArrayList<>();
        if(filter.locationType()!=null){where.append(" AND LocationType=?");p.add(filter.locationType());}
        if(filter.locationId()!=null){where.append("BRANCH".equals(filter.locationType())?" AND BranchID=?":" AND WarehouseID=?");p.add(filter.locationId());}
        applySessionScope(where,p);
        return queryDAO.queryAll("SELECT CategoryName Category,SUM(Quantity) Quantity,SUM(CostValue) Value FROM v_current_inventory"+where+" GROUP BY CategoryName ORDER BY Quantity DESC",p);
    }

    public List<QueryDAO.Option> branches()throws SQLException{return queryDAO.options("SELECT BranchID,Name FROM branches WHERE IsActive=1 ORDER BY Name",List.of());}
    public List<QueryDAO.Option> warehouses()throws SQLException{return queryDAO.options("SELECT WarehouseID,Name FROM warehouses WHERE IsActive=1 ORDER BY Name",List.of());}
    public List<QueryDAO.Option> categories()throws SQLException{return queryDAO.options("SELECT CategoryID,CategoryName FROM categories WHERE IsActive=1 ORDER BY CategoryName",List.of());}

    public void adjust(StockOperation operation)throws SQLException{
        AuthorizationService.require("MANAGE_INVENTORY");
        if(operation.quantity()==0)throw new IllegalArgumentException("Adjustment quantity cannot be zero.");
        try(Connection c=DatabaseConnection.open()){c.setAutoCommit(false);try{
            int old=lockQuantity(c,operation.locationType(),operation.locationId(),operation.variantId());int updated=old+operation.quantity();if(updated<0)throw new IllegalArgumentException("The adjustment would create negative stock.");
            updateQuantity(c,operation.locationType(),operation.locationId(),operation.variantId(),updated);
            insertMovement(c,operation.locationType(),operation.locationId(),operation.variantId(),"ADJUSTMENT",operation.quantity()>0?"IN":"OUT",Math.abs(operation.quantity()),"ADJUSTMENT",null,null,operation.reason());
            AuthDAO.insertAudit(c,AppSession.current().userId(),AppSession.current().username(),"INVENTORY_ADJUST","INVENTORY",operation.variantId(),"Stock "+old+" → "+updated+" at "+operation.locationType()+" "+operation.locationId()+": "+operation.reason(),true,null);c.commit();
        }catch(Exception e){c.rollback();if(e instanceof SQLException x)throw x;if(e instanceof IllegalArgumentException x)throw x;throw new SQLException(e.getMessage(),e);}finally{c.setAutoCommit(true);}}
    }

    public long transfer(TransferOperation operation)throws SQLException{
        AuthorizationService.require("MANAGE_INVENTORY");
        if(operation.quantity()<=0)throw new IllegalArgumentException("Transfer quantity must be greater than zero.");
        if(operation.fromType().equals(operation.toType())&&operation.fromId()==operation.toId())throw new IllegalArgumentException("Source and destination cannot be the same.");
        try(Connection c=DatabaseConnection.open()){c.setAutoCommit(false);try{
            int source=lockQuantity(c,operation.fromType(),operation.fromId(),operation.variantId());if(source<operation.quantity())throw new IllegalArgumentException("Insufficient source stock.");
            ensureInventoryRow(c,operation.toType(),operation.toId(),operation.variantId());int destination=lockQuantity(c,operation.toType(),operation.toId(),operation.variantId());
            updateQuantity(c,operation.fromType(),operation.fromId(),operation.variantId(),source-operation.quantity());updateQuantity(c,operation.toType(),operation.toId(),operation.variantId(),destination+operation.quantity());
            String reference="TR-"+System.currentTimeMillis();long transferId;
            try(PreparedStatement s=c.prepareStatement("INSERT INTO stock_transfers(TransferNumber,FromLocationType,FromLocationID,ToLocationType,ToLocationID,TransferDate,Status,CreatedBy,ApprovedBy,Notes) VALUES(?,?,?,?,?,NOW(),'COMPLETED',?,?,?)",Statement.RETURN_GENERATED_KEYS)){s.setString(1,reference);s.setString(2,operation.fromType());s.setInt(3,operation.fromId());s.setString(4,operation.toType());s.setInt(5,operation.toId());s.setInt(6,AppSession.current().userId());s.setInt(7,AppSession.current().userId());s.setString(8,operation.reason());s.executeUpdate();try(ResultSet keys=s.getGeneratedKeys()){keys.next();transferId=keys.getLong(1);}}
            try(PreparedStatement s=c.prepareStatement("INSERT INTO stock_transfer_items(TransferID,VariantID,Quantity) VALUES(?,?,?)")){s.setLong(1,transferId);s.setInt(2,operation.variantId());s.setInt(3,operation.quantity());s.executeUpdate();}
            insertMovement(c,operation.fromType(),operation.fromId(),operation.variantId(),"TRANSFER","OUT",operation.quantity(),"TRANSFER",transferId,reference,operation.reason());insertMovement(c,operation.toType(),operation.toId(),operation.variantId(),"TRANSFER","IN",operation.quantity(),"TRANSFER",transferId,reference,operation.reason());
            AuthDAO.insertAudit(c,AppSession.current().userId(),AppSession.current().username(),"STOCK_TRANSFER","TRANSFER",(int)transferId,"Transferred "+operation.quantity()+" units; "+reference,true,null);c.commit();return transferId;
        }catch(Exception e){c.rollback();if(e instanceof SQLException x)throw x;if(e instanceof IllegalArgumentException x)throw x;throw new SQLException(e.getMessage(),e);}finally{c.setAutoCommit(true);}}
    }

    public void markDamaged(StockOperation operation)throws SQLException{
        AuthorizationService.require("MANAGE_INVENTORY");if(operation.quantity()<=0)throw new IllegalArgumentException("Damaged quantity must be greater than zero.");
        try(Connection c=DatabaseConnection.open()){c.setAutoCommit(false);try{int old=lockQuantity(c,operation.locationType(),operation.locationId(),operation.variantId());if(old<operation.quantity())throw new IllegalArgumentException("Damaged quantity exceeds available stock.");updateQuantity(c,operation.locationType(),operation.locationId(),operation.variantId(),old-operation.quantity());try(PreparedStatement s=c.prepareStatement("INSERT INTO damaged_stock(LocationType,LocationID,VariantID,Quantity,Reason,RecordedBy,RecordedAt) VALUES(?,?,?,?,?,?,NOW())")){s.setString(1,operation.locationType());s.setInt(2,operation.locationId());s.setInt(3,operation.variantId());s.setInt(4,operation.quantity());s.setString(5,operation.reason());s.setInt(6,AppSession.current().userId());s.executeUpdate();}insertMovement(c,operation.locationType(),operation.locationId(),operation.variantId(),"DAMAGED","OUT",operation.quantity(),"DAMAGED",null,null,operation.reason());AuthDAO.insertAudit(c,AppSession.current().userId(),AppSession.current().username(),"DAMAGED_STOCK","INVENTORY",operation.variantId(),"Marked "+operation.quantity()+" damaged: "+operation.reason(),true,null);c.commit();}catch(Exception e){c.rollback();if(e instanceof SQLException x)throw x;if(e instanceof IllegalArgumentException x)throw x;throw new SQLException(e.getMessage(),e);}finally{c.setAutoCommit(true);}}
    }

    public List<Map<String,Object>> movementHistory(int variantId)throws SQLException{return queryDAO.queryAll("SELECT MovementDate AS Date,LocationType AS `Location Type`,LocationID AS `Location ID`,MovementType AS Type,Direction,Quantity,ReferenceNumber AS Reference,Notes FROM stock_movements WHERE VariantID=? ORDER BY MovementDate DESC LIMIT 200",List.of(variantId));}
    private int lockQuantity(Connection c,String type,int locationId,int variantId)throws SQLException{String table="BRANCH".equals(type)?"branch_inventory":"warehouse_inventory";String id="BRANCH".equals(type)?"BranchID":"WarehouseID";try(PreparedStatement s=c.prepareStatement("SELECT Quantity FROM "+table+" WHERE "+id+"=? AND VariantID=? FOR UPDATE")){s.setInt(1,locationId);s.setInt(2,variantId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Inventory row not found.");return r.getInt(1);}}}
    private void updateQuantity(Connection c,String type,int locationId,int variantId,int quantity)throws SQLException{String table="BRANCH".equals(type)?"branch_inventory":"warehouse_inventory";String id="BRANCH".equals(type)?"BranchID":"WarehouseID";try(PreparedStatement s=c.prepareStatement("UPDATE "+table+" SET Quantity=?,UpdatedAt=NOW() WHERE "+id+"=? AND VariantID=?")){s.setInt(1,quantity);s.setInt(2,locationId);s.setInt(3,variantId);if(s.executeUpdate()!=1)throw new SQLException("Inventory update failed.");}}
    private void ensureInventoryRow(Connection c,String type,int locationId,int variantId)throws SQLException{String table="BRANCH".equals(type)?"branch_inventory":"warehouse_inventory";String id="BRANCH".equals(type)?"BranchID":"WarehouseID";try(PreparedStatement s=c.prepareStatement("INSERT IGNORE INTO "+table+"("+id+",VariantID,Quantity,ReorderLevel,UpdatedAt) VALUES(?,?,0,5,NOW())")){s.setInt(1,locationId);s.setInt(2,variantId);s.executeUpdate();}}
    private void insertMovement(Connection c,String type,int locationId,int variantId,String movementType,String direction,int quantity,String refType,Long refId,String refNumber,String notes)throws SQLException{try(PreparedStatement s=c.prepareStatement("INSERT INTO stock_movements(LocationType,LocationID,VariantID,MovementType,Direction,Quantity,ReferenceType,ReferenceID,ReferenceNumber,MovementDate,PerformedBy,Notes) VALUES(?,?,?,?,?,?,?,?,?,NOW(),?,?)")){s.setString(1,type);s.setInt(2,locationId);s.setInt(3,variantId);s.setString(4,movementType);s.setString(5,direction);s.setInt(6,quantity);s.setString(7,refType);if(refId==null)s.setNull(8,Types.BIGINT);else s.setLong(8,refId);s.setString(9,refNumber);s.setInt(10,AppSession.current().userId());s.setString(11,notes);s.executeUpdate();}}

    public record InventoryFilter(String keyword,String locationType,Integer locationId,String category,String stockStatus,Integer minimumQuantity,Integer maximumQuantity,LocalDate updatedFrom,LocalDate updatedTo){}
    public record Summary(long quantity,BigDecimal costValue,BigDecimal retailValue,int lowStock,int outOfStock,int overstock){}
    public record StockOperation(String locationType,int locationId,int variantId,int quantity,String reason){}
    public record TransferOperation(String fromType,int fromId,String toType,int toId,int variantId,int quantity,String reason){}
}
