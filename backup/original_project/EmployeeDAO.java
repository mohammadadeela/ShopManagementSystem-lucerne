import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class EmployeeDAO {
    public static ObservableList<Employee> getAllEmployees() {
        ObservableList<Employee> employees = FXCollections.observableArrayList();
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT e.EmployeeID, e.UserID, u.FullName, u.Username, u.Role, " +
                    "e.BranchID, e.WarehouseID, e.JobTitle, e.Salary " +
                    "FROM employees e JOIN users u ON e.UserID = u.UserID"
            );
            while (rs.next()) {
                Integer branchId = rs.getObject("BranchID") == null ? null : rs.getInt("BranchID");
                Integer warehouseId = rs.getObject("WarehouseID") == null ? null : rs.getInt("WarehouseID");
                employees.add(new Employee(
                        rs.getInt("EmployeeID"),
                        rs.getInt("UserID"),
                        rs.getString("FullName"),
                        rs.getString("Username"),
                        rs.getString("Role"),
                        branchId,
                        warehouseId,
                        rs.getString("JobTitle"),
                        rs.getDouble("Salary")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return employees;
    }
}
