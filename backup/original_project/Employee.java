public class Employee {
    private int employeeId;
    private int userId;
    private String fullName;
    private String username;
    private String role;
    private Integer branchId;
    private Integer warehouseId;
    private String jobTitle;
    private double salary;

    public Employee(int employeeId, int userId, String fullName, String username,
                    String role, Integer branchId, Integer warehouseId,
                    String jobTitle, double salary) {
        this.employeeId = employeeId;
        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
        this.role = role;
        this.branchId = branchId;
        this.warehouseId = warehouseId;
        this.jobTitle = jobTitle;
        this.salary = salary;
    }

    public int getEmployeeId() { return employeeId; }
    public int getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public Integer getBranchId() { return branchId; }
    public Integer getWarehouseId() { return warehouseId; }
    public String getJobTitle() { return jobTitle; }
    public double getSalary() { return salary; }
    public String toString() { return fullName + " - " + jobTitle; }
}
