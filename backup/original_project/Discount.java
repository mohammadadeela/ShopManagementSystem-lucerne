import java.sql.Date;

public class Discount {
    private int discountId;
    private String code;
    private double percentage;
    private Date startDate;
    private Date endDate;

    public Discount(int discountId, String code, double percentage, Date startDate, Date endDate) {
        this.discountId = discountId;
        this.code = code;
        this.percentage = percentage;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public int getDiscountId() { return discountId; }
    public String getCode() { return code; }
    public double getPercentage() { return percentage; }
    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }
}
