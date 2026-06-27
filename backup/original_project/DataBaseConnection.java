public class DataBaseConnection {
    private String dbName = "lucerne_demo";
    private String username = "root";
    private String password = "28120261103mk";
    private String port = "3306";
    private String url = "localhost";

    public DBConn getConnection() {
        return new DBConn(dbName, username, password, port, url);
    }
}
