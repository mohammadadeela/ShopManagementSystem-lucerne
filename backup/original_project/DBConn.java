import java.sql.Connection;
import java.sql.DriverManager;

public class DBConn {
    private Connection connection;

    public DBConn(String dbName, String username, String password, String port, String url) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String fullUrl = "jdbc:mysql://" + url + ":" + port + "/" + dbName
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            connection = DriverManager.getConnection(fullUrl, username, password);
            System.out.println("Database connected successfully");
        } catch (Exception e) {
            e.printStackTrace();
            connection = null;
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
