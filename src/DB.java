// import java.sql.Connection;
// import java.sql.DriverManager;

// public class DB {

//     private static final String URL = "jdbc:mysql://localhost:3306/litecore_db";
//     private static final String USER = "root";
//     private static final String PASS = "10863geo*";  // XAMPP default password is empty

//     public static Connection getConnection() throws Exception {
//         return DriverManager.getConnection(URL, USER, PASS);
//     }
// }
import java.sql.Connection;
import java.sql.DriverManager;

public class DB {

    private static final String URL = "jdbc:mysql://localhost:3306/litecore_db";
    private static final String USER = "root";
    private static final String PASS = "10863geo*";  // change if you set MySQL password

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
