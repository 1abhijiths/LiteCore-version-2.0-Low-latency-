// import java.util.*;
// import java.sql.*;
// import java.security.MessageDigest;
// import java.security.SecureRandom;

// public class Main {

//     // Store token → username
//     private static Map<String, String> tokenStore = new HashMap<>();

//     public static void main(String[] args) {

//         DBInitializer.initialize();
//         LiteCore app = new LiteCore();

//         // ======================================================
//         // GLOBAL LOGGING MIDDLEWARE
//         // ======================================================
//         app.use((req, res, next) -> {
//             System.out.println("[LOG] " + req.method + " " + req.path);
//             next.run();
//         });
        
//     app.use((req, res, next) -> {
//     long start = System.nanoTime();
//     next.run();
//     long duration = System.nanoTime() - start;
//     System.out.println("[LATENCY] " + req.method + " " + req.path + " = " + (duration / 1_000_000.0) + " ms");
// });

//         // ======================================================
//         // AUTH MIDDLEWARE
//         // ======================================================
//         LiteCore.Middleware auth = (req, res, next) -> {
//             String token = req.headers.get("Authorization");

//             if (token == null || !tokenStore.containsKey(token)) {
//                 res.status(401).json("{\"error\":\"Unauthorized - invalid token\"}");
//                 return;
//             }

//             req.user = tokenStore.get(token);  // attach username
//             next.run();
//         };

//         // ======================================================
//         // REGISTER USER
//         // ======================================================
//         app.post("/register", (req, res) -> {

//             Map<String, String> data = parseForm(req.body);
//             String username = data.get("username");
//             String password = data.get("password");

//             if (username == null || password == null) {
//                 res.status(400).json("{\"error\":\"Missing username or password\"}");
//                 return;
//             }

//             String hashedPassword = hash(password);

//             try (Connection conn = DBPool.getConnection()) {

//                 PreparedStatement check = conn.prepareStatement(
//                     "SELECT * FROM users WHERE username=?"
//                 );
//                 check.setString(1, username);
//                 ResultSet rs = check.executeQuery();
//                 System.out.println("helo");
//                 if (rs.next()) {
//                     res.status(409).json("{\"error\":\"User already exists\"}");
//                     return;
//                 }

//                 PreparedStatement stmt = conn.prepareStatement(
//                     "INSERT INTO users(username, password) VALUES(?, ?)"
//                 );
//                 stmt.setString(1, username);
//                 stmt.setString(2, hashedPassword);
//                 stmt.executeUpdate();

//                 res.json("{\"message\":\"User registered successfully\"}");

//             } catch (Exception e) {
//                 res.status(500).json("{\"error\":\"Database error\"}");
//             }
//         });

//         // ======================================================
//         // LOGIN USER
//         // ======================================================
//         app.post("/login", (req, res) -> {

//             Map<String, String> data = parseForm(req.body);
//             String username = data.get("username");
//             String password = data.get("password");

//             if (username == null || password == null) {
//                 res.status(400).json("{\"error\":\"Missing username or password\"}");
//                 return;
//             }

//             String hashedInput = hash(password);

//             try (Connection conn = DBPool.getConnection()) {

//                 PreparedStatement stmt = conn.prepareStatement(
//                     "SELECT * FROM users WHERE username=? AND password=?"
//                 );
//                 stmt.setString(1, username);
//                 stmt.setString(2, hashedInput);
//                 ResultSet rs = stmt.executeQuery();

//                 if (rs.next()) {
//                     String token = generateToken();
//                     tokenStore.put(token, username);

//                     res.json("{\"message\":\"Login successful\", \"token\":\"" + token + "\"}");
//                 } else {
//                     res.status(401).json("{\"error\":\"Invalid credentials\"}");
//                 }

//             } catch (Exception e) {
//                 res.status(500).json("{\"error\":\"Database error\"}");
//             }
//         });

//         // ======================================================
//         // PROTECTED: GET ALL USERS
//         // ======================================================
//         app.get("/users", auth, (req, res) -> {

//             try (Connection conn = DBPool.getConnection()) {

//                 Statement stmt = conn.createStatement();
//                 ResultSet rs = stmt.executeQuery("SELECT username FROM users");

//                 StringBuilder out = new StringBuilder("[");
//                 boolean first = true;

//                 while (rs.next()) {
//                     if (!first) out.append(",");
//                     first = false;

//                     out.append("{\"username\":\"")
//                        .append(rs.getString("username"))
//                        .append("\"}");
//                 }

//                 out.append("]");
//                 res.json(out.toString());

//             } catch (Exception e) {
//                 res.status(500).json("{\"error\":\"Database error\"}");
//             }
//         });

//         // ======================================================
//         // PROTECTED: UPDATE PASSWORD
//         // ======================================================
//         app.put("/update", auth, (req, res) -> {

//             Map<String, String> data = parseForm(req.body);
//             String oldPassword = data.get("oldPassword");
//             String newPassword = data.get("newPassword");
//             String username = req.user;

//             if (oldPassword == null || newPassword == null) {
//                 res.status(400).json("{\"error\":\"Missing password fields\"}");
//                 return;
//             }

//             String oldHash = hash(oldPassword);
//             String newHash = hash(newPassword);

//             try (Connection conn = DBPool.getConnection()) {

//                 PreparedStatement stmt = conn.prepareStatement(
//                     "UPDATE users SET password=? WHERE username=? AND password=?"
//                 );
//                 stmt.setString(1, newHash);
//                 stmt.setString(2, username);
//                 stmt.setString(3, oldHash);

//                 int rows = stmt.executeUpdate();

//                 if (rows > 0) {
//                     res.json("{\"message\":\"Password updated successfully\"}");
//                 } else {
//                     res.status(401).json("{\"error\":\"Incorrect old password\"}");
//                 }

//             } catch (Exception e) {
//                 res.status(500).json("{\"error\":\"Database error\"}");
//             }
//         });

//         // ======================================================
//         // PROTECTED: DELETE ACCOUNT
//         // ======================================================
//         app.delete("/delete", auth, (req, res) -> {

//             String username = req.user;

//             try (Connection conn = DBPool.getConnection()) {

//                 PreparedStatement stmt = conn.prepareStatement(
//                     "DELETE FROM users WHERE username=?"
//                 );
//                 stmt.setString(1, username);

//                 int rows = stmt.executeUpdate();

//                 if (rows > 0) {
//                     res.json("{\"message\":\"Account deleted\"}");
//                 } else {
//                     res.status(404).json("{\"error\":\"User not found\"}");
//                 }

//             } catch (Exception e) {
//                 res.status(500).json("{\"error\":\"Database error\"}");
//             }
//         });

//         // Start server
//         app.start(8080);
//     }

//     // ======================================================
//     // Helper: Parse form data
//     // ======================================================
//     private static Map<String, String> parseForm(String body) {
//         Map<String, String> map = new HashMap<>();
//         if (body == null || body.isEmpty()) return map;

//         String[] pairs = body.split("&");
//         for (String p : pairs) {
//             String[] kv = p.split("=", 2);
//             if (kv.length == 2) map.put(kv[0], kv[1]);
//         }
//         return map;
//     }

//     // ======================================================
//     // Helper: Hash password using SHA-256
//     // ======================================================
//     private static String hash(String password) {
//         try {
//             MessageDigest digest = MessageDigest.getInstance("SHA-256");
//             byte[] bytes = digest.digest(password.getBytes("UTF-8"));

//             StringBuilder hex = new StringBuilder();
//             for (byte b : bytes) hex.append(String.format("%02x", b));
//             return hex.toString();

//         } catch (Exception e) {
//             return password;
//         }
//     }

//     // ======================================================
//     // Helper: Generate random token
//     // ======================================================
//     private static String generateToken() {
//         byte[] bytes = new byte[24];
//         new SecureRandom().nextBytes(bytes);

//         StringBuilder sb = new StringBuilder();
//         for (byte b : bytes) sb.append(String.format("%02x", b));

//         return sb.toString();
//     }
// }

import java.util.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main {

    private static Map<String, String> tokenStore = new HashMap<>();

    public static void main(String[] args) {

        // Initialize DB (users + cart table)
        DBInitializer.initialize();

        LiteCore app = new LiteCore();

        // ------------------------------------------------------------
        // GLOBAL LOGGING MIDDLEWARE
        // ------------------------------------------------------------
        app.use((req, res, next) -> {
            System.out.println("[LOG] " + req.method + " " + req.path);
            long start = System.nanoTime();
            next.run();
            long end = System.nanoTime();
            System.out.println("[LATENCY] " + req.method + " " + req.path + " = " 
                               + ((end - start) / 1_000_000.0) + " ms");
        });

        // ------------------------------------------------------------
        // AUTH MIDDLEWARE
        // ------------------------------------------------------------
        LiteCore.Middleware auth = (req, res, next) -> {
            String token = req.headers.get("Authorization");
            if (token == null || !tokenStore.containsKey(token)) {
                res.status(401).json("{\"error\":\"Unauthorized - Invalid Token\"}");
                return;
            }
            req.user = tokenStore.get(token);
            next.run();
        };

        // ------------------------------------------------------------
        // REGISTER USER
        // ------------------------------------------------------------
        app.post("/register", (req, res) -> {
            Map<String, String> form = parseForm(req.body);
            String username = form.get("username");
            String password = form.get("password");

            if (username == null || password == null) {
                res.status(400).json("{\"error\":\"Missing username or password\"}");
                return;
            }

            String hashed = hash(password);

            try (Connection conn = DBPool.getConnection()) {

                PreparedStatement check = conn.prepareStatement(
                    "SELECT * FROM users WHERE username=?"
                );
                check.setString(1, username);
                ResultSet rs = check.executeQuery();

                if (rs.next()) {
                    res.status(409).json("{\"error\":\"User already exists\"}");
                    return;
                }

                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users(username, password) VALUES (?, ?)"
                );
                stmt.setString(1, username);
                stmt.setString(2, hashed);
                stmt.executeUpdate();

                res.json("{\"status\":\"success\",\"message\":\"User registered\"}");

            } catch (Exception e) {
                e.printStackTrace();
                res.status(500).json("{\"error\":\"DB error\"}");
            }
        });

        // ------------------------------------------------------------
        // LOGIN
        // ------------------------------------------------------------
        app.post("/login", (req, res) -> {
            Map<String, String> form = parseForm(req.body);
            String username = form.get("username");
            String password = form.get("password");

            if (username == null || password == null) {
                res.status(400).json("{\"error\":\"Missing username or password\"}");
                return;
            }

            String hashed = hash(password);

            try (Connection conn = DBPool.getConnection()) {

                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM users WHERE username=? AND password=?"
                );
                stmt.setString(1, username);
                stmt.setString(2, hashed);

                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String token = generateToken();
                    tokenStore.put(token, username);

                    res.json("{\"status\":\"success\",\"token\":\"" + token + "\"}");
                } else {
                    res.status(401).json("{\"error\":\"Invalid credentials\"}");
                }

            } catch (Exception e) {
                res.status(500).json("{\"error\":\"DB error\"}");
            }
        });

        // ------------------------------------------------------------
        // PROTECTED: VIEW ALL USERS
        // ------------------------------------------------------------
        app.get("/users", auth, (req, res) -> {
            try (Connection conn = DBPool.getConnection()) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT username FROM users");

                StringBuilder sb = new StringBuilder("[");
                boolean first = true;

                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"username\":\"")
                      .append(rs.getString("username"))
                      .append("\"}");
                }

                sb.append("]");
                res.json(sb.toString());

            } catch (Exception e) {
                res.status(500).json("{\"error\":\"DB error\"}");
            }
        });

        // ------------------------------------------------------------
        // PROTECTED: UPDATE PASSWORD
        // ------------------------------------------------------------
        app.put("/update", auth, (req, res) -> {
            Map<String, String> form = parseForm(req.body);
            String oldP = form.get("oldPassword");
            String newP = form.get("newPassword");

            if (oldP == null || newP == null) {
                res.status(400).json("{\"error\":\"Missing fields\"}");
                return;
            }

            String oldHash = hash(oldP);
            String newHash = hash(newP);

            try (Connection conn = DBPool.getConnection()) {

                PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE users SET password=? WHERE username=? AND password=?"
                );
                stmt.setString(1, newHash);
                stmt.setString(2, req.user);
                stmt.setString(3, oldHash);

                int rows = stmt.executeUpdate();

                if (rows > 0) {
                    res.json("{\"status\":\"success\",\"message\":\"Password updated\"}");
                } else {
                    res.status(401).json("{\"error\":\"Incorrect old password\"}");
                }

            } catch (Exception e) {
                res.status(500).json("{\"error\":\"DB error\"}");
            }
        });

        // ------------------------------------------------------------
        // PROTECTED: DELETE USER
        // ------------------------------------------------------------
        app.delete("/delete", auth, (req, res) -> {
            try (Connection conn = DBPool.getConnection()) {

                PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM users WHERE username=?"
                );
                stmt.setString(1, req.user);

                int rows = stmt.executeUpdate();

                if (rows > 0) {
                    res.json("{\"status\":\"success\",\"message\":\"User deleted\"}");
                } else {
                    res.status(404).json("{\"error\":\"User not found\"}");
                }

            } catch (Exception e) {
                res.status(500).json("{\"error\":\"DB error\"}");
            }
        });

        // ------------------------------------------------------------
        // 🔥 CART SYSTEM
        // ------------------------------------------------------------

        // ADD to cart
        app.post("/cart/add", auth, (req, res) -> {
            Map<String, String> form = parseForm(req.body);
            String product = form.get("product");

            if (product == null || product.isEmpty()) {
                res.status(400).json("{\"error\":\"Missing product\"}");
                return;
            }

            try (Connection conn = DBPool.getConnection()) {

                PreparedStatement getUid = conn.prepareStatement(
                    "SELECT id FROM users WHERE username=?"
                );
                getUid.setString(1, req.user);
                ResultSet rs = getUid.executeQuery();
                rs.next();
                int userId = rs.getInt("id");

                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO cart(user_id, product) VALUES (?, ?)"
                );
                stmt.setInt(1, userId);
                stmt.setString(2, product);
                stmt.executeUpdate();

                res.json("{\"status\":\"success\",\"message\":\"Added to cart\"}");

            } catch (Exception e) {
                res.status(500).json("{\"error\":\"DB error\"}");
            }
        });

        // VIEW cart
        app.get("/cart", auth, (req, res) -> {
            try (Connection conn = DBPool.getConnection()) {

                PreparedStatement getUid = conn.prepareStatement(
                    "SELECT id FROM users WHERE username=?"
                );
                getUid.setString(1, req.user);
                ResultSet rs = getUid.executeQuery();
                rs.next();
                int userId = rs.getInt("id");

                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT product FROM cart WHERE user_id=?"
                );
                stmt.setInt(1, userId);
                ResultSet rs2 = stmt.executeQuery();

                StringBuilder sb = new StringBuilder("[");
                boolean first = true;

                while (rs2.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"product\":\"")
                      .append(rs2.getString("product"))
                      .append("\"}");
                }

                sb.append("]");
                res.json(sb.toString());

            } catch (Exception e) {
                res.status(500).json("{\"error\":\"DB error\"}");
            }
        });

        // REMOVE from cart
        app.delete("/cart/remove", auth, (req, res) -> {
            Map<String, String> form = parseForm(req.body);
            String product = form.get("product");

            if (product == null) {
                res.status(400).json("{\"error\":\"Missing product name\"}");
                return;
            }

            try (Connection conn = DBPool.getConnection()) {

                PreparedStatement getUid = conn.prepareStatement(
                    "SELECT id FROM users WHERE username=?"
                );
                getUid.setString(1, req.user);
                ResultSet rs = getUid.executeQuery();
                rs.next();
                int userId = rs.getInt("id");

                PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM cart WHERE user_id=? AND product=? LIMIT 1"
                );
                stmt.setInt(1, userId);
                stmt.setString(2, product);

                int rows = stmt.executeUpdate();

                if (rows > 0) {
                    res.json("{\"status\":\"success\",\"message\":\"Product removed\"}");
                } else {
                    res.status(404).json("{\"error\":\"Product not found\"}");
                }

            } catch (Exception e) {
                res.status(500).json("{\"error\":\"DB error\"}");
            }
        });

        // ------------------------------------------------------------
        // START SERVER
        // ------------------------------------------------------------
        app.start(8080);
    }

    // ------------------------------------------------------------
    // HELPER FUNCTIONS
    // ------------------------------------------------------------
    
    private static String generateToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.isEmpty()) return map;
        String[] parts = body.split("&");
        for (String p : parts) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }

    private static String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : encoded) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return password;
        }
    }
}
