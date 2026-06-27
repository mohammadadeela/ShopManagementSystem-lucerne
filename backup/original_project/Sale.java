import java.sql.Date;

public class Sale {
    private int saleId;
    private int branchId;
    private Integer customerId;
    private int cashierUserId;
    private Integer discountId;
    private Date saleDate;
    private double totalAmount;
    private double discountAmount;
    private double finalAmount;

    public Sale(int saleId, int branchId, Integer customerId, int cashierUserId,
                Integer discountId, Date saleDate, double totalAmount,
                double discountAmount, double finalAmount) {
        this.saleId = saleId;
        this.branchId = branchId;
        this.customerId = customerId;
        this.cashierUserId = cashierUserId;
        this.discountId = discountId;
        this.saleDate = saleDate;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
    }

    public int getSaleId() { return saleId; }
    public int getBranchId() { return branchId; }
    public Integer getCustomerId() { return customerId; }
    public int getCashierUserId() { return cashierUserId; }
    public Integer getDiscountId() { return discountId; }
    public Date getSaleDate() { return saleDate; }
    public double getTotalAmount() { return totalAmount; }
    public double getDiscountAmount() { return discountAmount; }
    public double getFinalAmount() { return finalAmount; }
}
