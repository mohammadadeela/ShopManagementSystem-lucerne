import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ProductDAO {
    public static ObservableList<Product> getAllProducts() {
        ObservableList<Product> products = FXCollections.observableArrayList();
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT ProductID, Name, Category, Price, CostPrice FROM products");
            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("ProductID"),
                        rs.getString("Name"),
                        rs.getString("Category"),
                        rs.getDouble("Price"),
                        rs.getDouble("CostPrice")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return products;
    }
}
