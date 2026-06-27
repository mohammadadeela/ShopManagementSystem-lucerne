public class Branch {
    private int branchId;
    private String name;
    private String location;

    public Branch(int branchId, String name, String location) {
        this.branchId = branchId;
        this.name = name;
        this.location = location;
    }

    public int getBranchId() { return branchId; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String toString() { return name; }
}
