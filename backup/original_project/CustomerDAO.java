import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class CustomerDAO {
    public static ObservableList<Customer> getAllCustomers() {
        ObservableList<Customer> customers = FXCollections.observableArrayList();
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT c.CustomerID, c.UserID, u.FullName, c.Phone " +
                    "FROM customers c JOIN users u ON c.UserID = u.UserID"
            );
            while (rs.next()) {
                customers.add(new Customer(
                        rs.getInt("CustomerID"),
                        rs.getInt("UserID"),
                        rs.getString("FullName"),
                        rs.getString("Phone")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customers;
    }
}
