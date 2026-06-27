public class Warehouse {
    private int warehouseId;
    private String name;
    private String location;

    public Warehouse(int warehouseId, String name, String location) {
        this.warehouseId = warehouseId;
        this.name = name;
        this.location = location;
    }

    public int getWarehouseId() { return warehouseId; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String toString() { return name; }
}
