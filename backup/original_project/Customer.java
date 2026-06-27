public class Customer {
    private int customerId;
    private int userId;
    private String fullName;
    private String phone;

    public Customer(int customerId, int userId, String fullName, String phone) {
        this.customerId = customerId;
        this.userId = userId;
        this.fullName = fullName;
        this.phone = phone;
    }

    public int getCustomerId() { return customerId; }
    public int getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String toString() { return fullName; }
}
