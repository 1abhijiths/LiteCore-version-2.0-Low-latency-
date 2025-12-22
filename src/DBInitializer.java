// import java.sql.Connection;
// import java.sql.DriverManager;
// import java.sql.Statement;

// public class DBInitializer {

//     private static final String DB_NAME = "litecore_db";
//     private static final String USER = "root";
//     private static final String PASS = "10863geo*";

//     public static void initialize() {
//         try {
//             // STEP 1: Connect to MySQL server (without selecting DB)
//             Connection conn = DriverManager.getConnection(
//                 "jdbc:mysql://localhost:3306/?user=" + USER + "&password=" + PASS
//             );

//             Statement stmt = conn.createStatement();

//             // STEP 2: Create database if it does not exist
//             stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
//             System.out.println("✔ Database ready: " + DB_NAME);

//             conn.close();

//             // STEP 3: Connect to the specific database
//             Connection dbConn = DriverManager.getConnection(
//                 "jdbc:mysql://localhost:3306/" + DB_NAME + "?user=" + USER + "&password=" + PASS
//             );

//             Statement dbStmt = dbConn.createStatement();

//             // STEP 4: Create users table if it does not exist
//             dbStmt.executeUpdate(
//                 "CREATE TABLE IF NOT EXISTS users (" +
//                 "id INT AUTO_INCREMENT PRIMARY KEY," +
//                 "username VARCHAR(100)," +
//                 "password VARCHAR(300)" +
//                 ")"
//             );

//             System.out.println("✔ Users table ready.");

//             dbConn.close();

//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }
// }
// import java.sql.Connection;
// import java.sql.DriverManager;
// import java.sql.Statement;

// public class DBInitializer {

//     private static final String DB_NAME = "litecore_db";
//     private static final String USER = "root";
//     private static final String PASS = "10863geo*";

//     public static void initialize() {
//         try {
//             Connection conn = DriverManager.getConnection(
//                 "jdbc:mysql://localhost:3306/?user=" + USER + "&password=" + PASS
//             );

//             Statement stmt = conn.createStatement();
//             stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
//             conn.close();

//             Connection dbConn = DriverManager.getConnection(
//                 "jdbc:mysql://localhost:3306/" + DB_NAME + "?user=" + USER + "&password=" + PASS
//             );

//             Statement dbStmt = dbConn.createStatement();
//             dbStmt.executeUpdate(
//                 "CREATE TABLE IF NOT EXISTS users (" +
//                 "id INT AUTO_INCREMENT PRIMARY KEY," +
//                 "username VARCHAR(100)," +
//                 "password VARCHAR(300)" +
//                 ")"
//             );

//             dbConn.close();

//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }
// }
// import java.sql.Connection;
// import java.sql.Statement;

// public class DBInitializer {

//     public static void initialize() {
//         System.out.println(">> Initializing database...");

//         try (Connection conn = DBPool.getConnection()) {

//             Statement stmt = conn.createStatement();

//             // Create table if not exists
//             stmt.executeUpdate(
//                 "CREATE TABLE IF NOT EXISTS users (" +
//                 "id INT AUTO_INCREMENT PRIMARY KEY," +
//                 "username VARCHAR(255) UNIQUE NOT NULL," +
//                 "password VARCHAR(255) NOT NULL" +
//                 ");"
//             );

//             System.out.println(">> Database ready.");

//         } catch (Exception e) {
//             e.printStackTrace();
//             System.out.println(">> Database initialization failed!");
//         }
//     }
// }
import java.sql.Connection;
import java.sql.Statement;

public class DBInitializer {

    public static void initialize() {
        System.out.println(">> Initializing database...");

        try (Connection conn = DBPool.getConnection()) {

            Statement stmt = conn.createStatement();

            // -------------------------
            // USERS TABLE
            // -------------------------
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "username VARCHAR(255) UNIQUE NOT NULL," +
                "password VARCHAR(255) NOT NULL" +
                ");"
            );

            // -------------------------
            // CART TABLE
            // -------------------------
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS cart (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "product VARCHAR(255) NOT NULL," +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ");"
            );

            System.out.println(">> Database ready.");

        } catch (Exception e) {
            System.out.println(">> Database initialization failed!");
            e.printStackTrace();
        }
    }
}

