package com.lucerne.service;

import com.lucerne.app.AppSession;
import com.lucerne.config.DatabaseConnection;
import com.lucerne.dao.AuthDAO;
import com.lucerne.model.CartItem;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import com.lucerne.util.LoggerUtil;

public final class SaleService {
    public SaleResult completeSale(int branchId,Integer customerId,List<CartItem> cart,Integer discountId,BigDecimal discountAmount,
                                   String paymentMethod,BigDecimal paidAmount) throws SQLException {
        AuthorizationService.require("CREATE_SALE");
        if(cart==null||cart.isEmpty())throw new IllegalArgumentException("The cart is empty.");
        BigDecimal gross=cart.stream().map(CartItem::lineTotal).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal discount=discountAmount==null?BigDecimal.ZERO:discountAmount;
        BigDecimal net=gross.subtract(discount);if(net.signum()<0)net=BigDecimal.ZERO;
        if("CASH".equals(paymentMethod)&&paidAmount.compareTo(net)<0)throw new IllegalArgumentException("Paid amount is below the sale total.");
        String receipt="LC-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+"-"+ThreadLocalRandom.current().nextInt(10,99);
        try(Connection c=DatabaseConnection.open()){
            c.setAutoCommit(false);try{
                BigDecimal cost=BigDecimal.ZERO;
                for(CartItem item:cart){
                    try(PreparedStatement stock=c.prepareStatement("SELECT bi.Quantity,p.CostPrice FROM branch_inventory bi JOIN product_variants pv ON pv.VariantID=bi.VariantID JOIN products p ON p.ProductID=pv.ProductID WHERE bi.BranchID=? AND bi.VariantID=? FOR UPDATE")){stock.setInt(1,branchId);stock.setInt(2,item.variantId());try(ResultSet r=stock.executeQuery()){if(!r.next()||r.getInt(1)<item.quantity())throw new SQLException("Insufficient stock for "+item.productName());cost=cost.add(r.getBigDecimal(2).multiply(BigDecimal.valueOf(item.quantity())));}}
                }
                BigDecimal grossProfit=net.subtract(cost);
                int saleId;
                try(PreparedStatement sale=c.prepareStatement("""
INSERT INTO sales(ReceiptNumber,BranchID,CustomerID,CashierUserID,SaleDate,GrossAmount,DiscountID,DiscountAmount,NetAmount,CostAmount,GrossProfit,PaymentMethod,Status,CreatedAt)
                        VALUES(?,?,?,?,NOW(),?,?,?,?,?,?,?,'COMPLETED',NOW())""",Statement.RETURN_GENERATED_KEYS)){sale.setString(1,receipt);sale.setInt(2,branchId);if(customerId==null)sale.setNull(3,Types.INTEGER);else sale.setInt(3,customerId);sale.setInt(4,AppSession.current().userId());sale.setBigDecimal(5,gross);if(discountId==null)sale.setNull(6,Types.INTEGER);else sale.setInt(6,discountId);sale.setBigDecimal(7,discount);sale.setBigDecimal(8,net);sale.setBigDecimal(9,cost);sale.setBigDecimal(10,grossProfit);sale.setString(11,paymentMethod);sale.executeUpdate();try(ResultSet keys=sale.getGeneratedKeys()){keys.next();saleId=keys.getInt(1);}}
                try(PreparedStatement itemInsert=c.prepareStatement("INSERT INTO sale_items(SaleID,VariantID,Quantity,UnitPrice,DiscountAmount,LineTotal,CostAtSale) SELECT ?,?,?,?,0,?,p.CostPrice FROM product_variants pv JOIN products p ON p.ProductID=pv.ProductID WHERE pv.VariantID=?");PreparedStatement reduce=c.prepareStatement("UPDATE branch_inventory SET Quantity=Quantity-?,UpdatedAt=NOW() WHERE BranchID=? AND VariantID=? AND Quantity>=?");PreparedStatement movement=c.prepareStatement("INSERT INTO stock_movements(LocationType,LocationID,VariantID,MovementType,Direction,Quantity,ReferenceType,ReferenceID,ReferenceNumber,MovementDate,PerformedBy) VALUES('BRANCH',?,?, 'SALE','OUT',?,'SALE',?,?,NOW(),?)")){
                    for(CartItem item:cart){itemInsert.setInt(1,saleId);itemInsert.setInt(2,item.variantId());itemInsert.setInt(3,item.quantity());itemInsert.setBigDecimal(4,item.unitPrice());itemInsert.setBigDecimal(5,item.lineTotal());itemInsert.setInt(6,item.variantId());itemInsert.addBatch();reduce.setInt(1,item.quantity());reduce.setInt(2,branchId);reduce.setInt(3,item.variantId());reduce.setInt(4,item.quantity());reduce.addBatch();movement.setInt(1,branchId);movement.setInt(2,item.variantId());movement.setInt(3,item.quantity());movement.setInt(4,saleId);movement.setString(5,receipt);movement.setInt(6,AppSession.current().userId());movement.addBatch();}
                    itemInsert.executeBatch();int[] updated=reduce.executeBatch();for(int value:updated)if(value==0)throw new SQLException("Stock changed during checkout. Retry the sale.");movement.executeBatch();
                }
                BigDecimal change="CASH".equals(paymentMethod)?paidAmount.subtract(net):BigDecimal.ZERO;
                try(PreparedStatement payment=c.prepareStatement("INSERT INTO payments(SaleID,PaymentMethod,Amount,PaidAmount,ChangeAmount,PaymentDate,Status) VALUES(?,?,?,?,?,NOW(),'COMPLETED')")){payment.setInt(1,saleId);payment.setString(2,paymentMethod);payment.setBigDecimal(3,net);payment.setBigDecimal(4,paidAmount);payment.setBigDecimal(5,change);payment.executeUpdate();}
                if("CASH".equals(paymentMethod)){try(PreparedStatement cash=c.prepareStatement("INSERT INTO cash_drawer_movements(CashierUserID,BranchID,MovementType,Amount,MovementDate,ReferenceNumber,Notes) VALUES(?,?,'SALE',?,NOW(),?,'Point of sale payment')")){cash.setInt(1,AppSession.current().userId());cash.setInt(2,branchId);cash.setBigDecimal(3,net);cash.setString(4,receipt);cash.executeUpdate();}}
                AuthDAO.insertAudit(c,AppSession.current().userId(),AppSession.current().username(),"SALE_CREATE","SALE",saleId,"Completed receipt "+receipt,true,null);
                c.commit();return new SaleResult(saleId,receipt,gross,discount,net,paidAmount,"CASH".equals(paymentMethod)?paidAmount.subtract(net):BigDecimal.ZERO);
            }catch(Exception e){c.rollback();LoggerUtil.warning(SaleService.class,"Sale transaction rolled back",e);if(e instanceof SQLException sql)throw sql;throw e;}finally{c.setAutoCommit(true);}
        }
    }

    public void cancelSale(long saleId, String reason) throws SQLException {
        AuthorizationService.require("CANCEL_SALE");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("A cancellation reason is required.");
        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                int branchId;
                int cashierId;
                String receipt;
                String paymentMethod;
                BigDecimal total;
                String status;
                try (PreparedStatement sale = connection.prepareStatement(
                        "SELECT BranchID,CashierUserID,ReceiptNumber,PaymentMethod,NetAmount,Status FROM sales WHERE SaleID=? FOR UPDATE")) {
                    sale.setLong(1, saleId);
                    try (ResultSet result = sale.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Sale was not found.");
                        branchId = result.getInt(1); cashierId = result.getInt(2); receipt = result.getString(3);
                        paymentMethod = result.getString(4); total = result.getBigDecimal(5); status = result.getString(6);
                    }
                }
                Integer assignedBranch = AppSession.current().branchId();
                if (assignedBranch != null && !AppSession.hasPermission("VIEW_ALL_BRANCHES") && assignedBranch != branchId)
                    throw new SecurityException("This sale belongs to another branch.");
                if (!"COMPLETED".equals(status)) throw new IllegalArgumentException("Only a completed sale can be cancelled.");
                try (PreparedStatement returned = connection.prepareStatement(
                        "SELECT COALESCE(SUM(ReturnedQuantity),0) FROM sale_items WHERE SaleID=?")) {
                    returned.setLong(1, saleId);
                    try (ResultSet result = returned.executeQuery()) { result.next(); if (result.getInt(1) > 0)
                        throw new IllegalArgumentException("A sale with returned items cannot be cancelled."); }
                }
                String itemsSql = "SELECT VariantID,Quantity FROM sale_items WHERE SaleID=? FOR UPDATE";
                try (PreparedStatement items = connection.prepareStatement(itemsSql);
                     PreparedStatement restore = connection.prepareStatement(
                             "INSERT INTO branch_inventory(BranchID,VariantID,Quantity,ReorderLevel,UpdatedAt) VALUES(?,?,?,5,NOW()) ON DUPLICATE KEY UPDATE Quantity=Quantity+VALUES(Quantity),UpdatedAt=NOW()");
                     PreparedStatement movement = connection.prepareStatement(
                             "INSERT INTO stock_movements(LocationType,LocationID,VariantID,MovementType,Direction,Quantity,ReferenceType,ReferenceID,ReferenceNumber,MovementDate,PerformedBy,Notes) VALUES('BRANCH',?,?,'SALE_CANCEL','IN',?,'SALE',?,?,NOW(),?,?)")) {
                    items.setLong(1, saleId);
                    try (ResultSet result = items.executeQuery()) {
                        while (result.next()) {
                            int variant = result.getInt(1), quantity = result.getInt(2);
                            restore.setInt(1, branchId); restore.setInt(2, variant); restore.setInt(3, quantity); restore.addBatch();
                            movement.setInt(1, branchId); movement.setInt(2, variant); movement.setInt(3, quantity);
                            movement.setLong(4, saleId); movement.setString(5, receipt); movement.setInt(6, AppSession.current().userId());
                            movement.setString(7, reason); movement.addBatch();
                        }
                    }
                    restore.executeBatch(); movement.executeBatch();
                }
                try (PreparedStatement update = connection.prepareStatement("UPDATE sales SET Status='CANCELLED' WHERE SaleID=?")) {
                    update.setLong(1, saleId); update.executeUpdate();
                }
                try (PreparedStatement payment = connection.prepareStatement("UPDATE payments SET Status='CANCELLED' WHERE SaleID=?")) {
                    payment.setLong(1, saleId); payment.executeUpdate();
                }
                if ("CASH".equals(paymentMethod)) {
                    try (PreparedStatement cash = connection.prepareStatement(
                            "INSERT INTO cash_drawer_movements(CashierUserID,BranchID,MovementType,Amount,MovementDate,ReferenceNumber,Notes) VALUES(?,?,'SALE_CANCEL',?,NOW(),?,?)")) {
                        cash.setInt(1, cashierId); cash.setInt(2, branchId); cash.setBigDecimal(3, total);
                        cash.setString(4, receipt); cash.setString(5, reason); cash.executeUpdate();
                    }
                }
                AuthDAO.insertAudit(connection, AppSession.current().userId(), AppSession.current().username(),
                        "SALE_CANCEL", "SALE", (int)saleId, "Cancelled " + receipt + ": " + reason, true, null);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                LoggerUtil.warning(SaleService.class, "Sale cancellation rolled back", exception);
                if (exception instanceof SQLException sql) throw sql;
                if (exception instanceof RuntimeException runtime) throw runtime;
                throw new SQLException(exception);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public record SaleResult(int saleId,String receiptNumber,BigDecimal gross,BigDecimal discount,BigDecimal total,BigDecimal paid,BigDecimal change){}
}
