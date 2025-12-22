// ============================================================================
// LiteCore - Lightweight Java Web Framework with Static File Serving
// ============================================================================

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import com.sun.net.httpserver.*;

// ============================================================================
// 1. CORE REQUEST CLASS
// ============================================================================
class Request {
    private final HttpExchange exchange;
    private Map<String, String> queryParams;
    private Map<String, String> pathParams;
    private String body;
    
    public Request(HttpExchange exchange) {
        this.exchange = exchange;
        this.pathParams = new HashMap<>();
        parseQueryParams();
    }
    
    public String getMethod() {
        return exchange.getRequestMethod();
    }
    
    public String getPath() {
        return exchange.getRequestURI().getPath();
    }
    
    public String getHeader(String key) {
        return exchange.getRequestHeaders().getFirst(key);
    }
    
    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        exchange.getRequestHeaders().forEach((key, values) -> 
            headers.put(key, values.get(0))
        );
        return headers;
    }
    
    public String getBody() {
        if (body == null) {
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                body = sb.toString();
            } catch (IOException e) {
                body = "";
            }
        }
        return body;
    }
    
    public Map<String, String> getQueryParams() {
        return queryParams;
    }
    
    public String getQueryParam(String key) {
        return queryParams.get(key);
    }
    
    public Map<String, String> getPathParams() {
        return pathParams;
    }
    
    public String getPathParam(String key) {
        return pathParams.get(key);
    }
    
    void setPathParams(Map<String, String> params) {
        this.pathParams = params;
    }
    
    private void parseQueryParams() {
        queryParams = new HashMap<>();
        String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    queryParams.put(pair[0], pair[1]);
                }
            }
        }
    }
}

// ============================================================================
// 2. CORE RESPONSE CLASS
// ============================================================================
class Response {
    private final HttpExchange exchange;
    private int statusCode = 200;
    private final Map<String, String> headers = new HashMap<>();
    private boolean sent = false;
    
    public Response(HttpExchange exchange) {
        this.exchange = exchange;
    }
    
    public Response status(int code) {
        this.statusCode = code;
        return this;
    }
    
    public Response header(String key, String value) {
        headers.put(key, value);
        return this;
    }
    
    public void json(Object data) {
        if (sent) return;
        header("Content-Type", "application/json");
        String json = toJson(data);
        send(json);
    }
    
    public void text(String text) {
        if (sent) return;
        header("Content-Type", "text/plain");
        send(text);
    }
    
    public void html(String html) {
        if (sent) return;
        header("Content-Type", "text/html");
        send(html);
    }
    
    // NEW: Method to send file content with appropriate MIME type
    public void sendFile(byte[] fileContent, String contentType) {
        if (sent) return;
        try {
            header("Content-Type", contentType);
            
            // Set headers
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                exchange.getResponseHeaders().set(entry.getKey(), entry.getValue());
            }
            
            exchange.sendResponseHeaders(statusCode, fileContent.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileContent);
            }
            sent = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void send(String body) {
        if (sent) return;
        try {
            // Set headers
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                exchange.getResponseHeaders().set(entry.getKey(), entry.getValue());
            }
            
            byte[] response = body.getBytes();
            exchange.sendResponseHeaders(statusCode, response.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
            sent = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + obj + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            Map<?, ?> map = (Map<?, ?>) obj;
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":")
                  .append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            List<?> list = (List<?>) obj;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + obj.toString() + "\"";
    }
}

// ============================================================================
// 3. ROUTE HANDLER INTERFACE
// ============================================================================
@FunctionalInterface
interface RouteHandler {
    void handle(Request req, Response res) throws Exception;
}

// ============================================================================
// 4. MIDDLEWARE INTERFACE
// ============================================================================
@FunctionalInterface
interface Middleware {
    boolean handle(Request req, Response res) throws Exception;
}

// ============================================================================
// 5. ROUTE CACHE (TLB-Style with LRU)
// ============================================================================
class RouteCache {
    private final int capacity;
    private final LinkedHashMap<String, RouteHandler> cache;
    
    public RouteCache(int capacity) {
        this.capacity = capacity;
        this.cache = new LinkedHashMap<String, RouteHandler>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, RouteHandler> eldest) {
                return size() > RouteCache.this.capacity;
            }
        };
    }
    
    public synchronized RouteHandler get(String key) {
        return cache.get(key);
    }
    
    public synchronized void put(String key, RouteHandler handler) {
        cache.put(key, handler);
    }
    
    public synchronized void clear() {
        cache.clear();
    }
    
    public synchronized int size() {
        return cache.size();
    }
}

// ============================================================================
// 6. ROUTE PATTERN MATCHER (for dynamic routes like /user/:id)
// ============================================================================
class RoutePattern {
    private final String pattern;
    private final List<String> paramNames;
    private final String regex;
    
    public RoutePattern(String pattern) {
        this.pattern = pattern;
        this.paramNames = new ArrayList<>();
        this.regex = buildRegex(pattern);
    }
    
    private String buildRegex(String pattern) {
        StringBuilder regex = new StringBuilder("^");
        String[] parts = pattern.split("/");
        
        for (String part : parts) {
            if (part.isEmpty()) continue;
            regex.append("/");
            
            if (part.startsWith(":")) {
                paramNames.add(part.substring(1));
                regex.append("([^/]+)");
            } else {
                regex.append(part);
            }
        }
        regex.append("$");
        return regex.toString();
    }
    
    public boolean matches(String path) {
        return path.matches(regex);
    }
    
    public Map<String, String> extractParams(String path) {
        Map<String, String> params = new HashMap<>();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher m = p.matcher(path);
        
        if (m.matches()) {
            for (int i = 0; i < paramNames.size(); i++) {
                params.put(paramNames.get(i), m.group(i + 1));
            }
        }
        return params;
    }
    
    public String getPattern() {
        return pattern;
    }
}

// ============================================================================
// 7. NEW: STATIC FILE SERVER
// ============================================================================
class StaticFileServer {
    private final String publicDir;
    private final Map<String, String> mimeTypes;
    
    public StaticFileServer(String publicDir) {
        this.publicDir = publicDir;
        this.mimeTypes = new HashMap<>();
        initializeMimeTypes();
    }
    
    private void initializeMimeTypes() {
        mimeTypes.put(".html", "text/html");
        mimeTypes.put(".css", "text/css");
        mimeTypes.put(".js", "application/javascript");
        mimeTypes.put(".json", "application/json");
        mimeTypes.put(".png", "image/png");
        mimeTypes.put(".jpg", "image/jpeg");
        mimeTypes.put(".jpeg", "image/jpeg");
        mimeTypes.put(".gif", "image/gif");
        mimeTypes.put(".svg", "image/svg+xml");
        mimeTypes.put(".ico", "image/x-icon");
        mimeTypes.put(".txt", "text/plain");
    }
    
    public boolean serveFile(String requestPath, Response res) {
        try {
            // If requesting root "/", serve index.html
            if (requestPath.equals("/") || requestPath.isEmpty()) {
                requestPath = "/index.html";
            }
            
            // Construct file path
            Path filePath = Paths.get(publicDir, requestPath);
            
            // Security check: prevent directory traversal
            if (!filePath.normalize().startsWith(Paths.get(publicDir).normalize())) {
                return false;
            }
            
            // Check if file exists
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                return false;
            }
            
            // Read file content
            byte[] fileContent = Files.readAllBytes(filePath);
            
            // Determine MIME type
            String contentType = getMimeType(filePath.toString());
            
            // Send file
            res.sendFile(fileContent, contentType);
            return true;
            
        } catch (IOException e) {
            System.err.println("Error serving static file: " + e.getMessage());
            return false;
        }
    }
    
    private String getMimeType(String filename) {
        for (Map.Entry<String, String> entry : mimeTypes.entrySet()) {
            if (filename.toLowerCase().endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "application/octet-stream"; // Default binary type
    }
}

// ============================================================================
// 8. MAIN LITECORE CLASS (MODIFIED)
// ============================================================================
class LiteCore {
    private HttpServer server;
    private final Map<String, RouteHandler> routes; // Main routing table
    private final RouteCache routeCache; // TLB-style cache
    private final List<Middleware> globalMiddleware;
    private final Map<String, RoutePattern> patternRoutes;
    private StaticFileServer staticFileServer; // NEW: Static file server
    private int port = 8080;
    
    public LiteCore() {
        this.routes = new HashMap<>();
        this.routeCache = new RouteCache(50); // Cache top 50 routes
        this.globalMiddleware = new ArrayList<>();
        this.patternRoutes = new HashMap<>();
    }
    
    // ========================================================================
    // NEW: STATIC FILE SERVING CONFIGURATION
    // ========================================================================
    
    /**
     * Enable static file serving from the specified directory
     * @param publicDir Path to the public directory (e.g., "public")
     */
    public void serveStatic(String publicDir) {
        this.staticFileServer = new StaticFileServer(publicDir);
        System.out.println("📁 Static files will be served from: " + publicDir);
    }
    
    // ========================================================================
    // ROUTING METHODS
    // ========================================================================
    
    public void get(String path, RouteHandler handler) {
        addRoute("GET", path, handler);
    }
    
    public void post(String path, RouteHandler handler) {
        addRoute("POST", path, handler);
    }
    
    public void put(String path, RouteHandler handler) {
        addRoute("PUT", path, handler);
    }
    
    public void delete(String path, RouteHandler handler) {
        addRoute("DELETE", path, handler);
    }
    
    private void addRoute(String method, String path, RouteHandler handler) {
        String key = method + ":" + path;
        routes.put(key, handler);
        
        // If path contains parameters, store pattern
        if (path.contains(":")) {
            patternRoutes.put(key, new RoutePattern(path));
        }
    }
    
    // ========================================================================
    // MIDDLEWARE METHODS
    // ========================================================================
    
    public void use(Middleware middleware) {
        globalMiddleware.add(middleware);
    }
    
    // ========================================================================
    // SERVER LIFECYCLE
    // ========================================================================
    
    public void start(int port) {
        this.port = port;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", this::handleRequest);
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();
            System.out.println("🚀 LiteCore server started on port " + port);
        } catch (IOException e) {
            System.err.println("❌ Failed to start server: " + e.getMessage());
        }
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("🛑 LiteCore server stopped");
        }
    }
    
    // ========================================================================
    // REQUEST HANDLING (MODIFIED)
    // ========================================================================
    
    private void handleRequest(HttpExchange exchange) {
        Request req = new Request(exchange);
        Response res = new Response(exchange);
        
        try {
            // Execute global middleware
            for (Middleware middleware : globalMiddleware) {
                if (!middleware.handle(req, res)) {
                    return; // Middleware stopped the chain
                }
            }
            
            // Route lookup
            String method = req.getMethod();
            String path = req.getPath();
            String routeKey = method + ":" + path;
            
            // Check TLB-style cache first
            RouteHandler handler = routeCache.get(routeKey);
            
            if (handler == null) {
                // Check main routing table
                handler = routes.get(routeKey);
                
                // Check pattern routes if not found
                if (handler == null) {
                    for (Map.Entry<String, RoutePattern> entry : patternRoutes.entrySet()) {
                        if (entry.getKey().startsWith(method + ":") && 
                            entry.getValue().matches(path)) {
                            handler = routes.get(entry.getKey());
                            req.setPathParams(entry.getValue().extractParams(path));
                            break;
                        }
                    }
                }
                
                // Cache the handler for future requests
                if (handler != null) {
                    routeCache.put(routeKey, handler);
                }
            }
            
            if (handler != null) {
                handler.handle(req, res);
            } else {
                // NEW: Before returning 404, try to serve static file
                if (staticFileServer != null && method.equals("GET")) {
                    boolean fileServed = staticFileServer.serveFile(path, res);
                    if (fileServed) {
                        return; // Static file was served successfully
                    }
                }
                
                // 404 Not Found (no route and no static file)
                res.status(404).json(Map.of(
                    "error", "Route not found",
                    "path", path,
                    "method", method
                ));
            }
            
        } catch (Exception e) {
            handleError(res, e);
        }
    }
    
    private void handleError(Response res, Exception e) {
        System.err.println("Error handling request: " + e.getMessage());
        e.printStackTrace();
        
        res.status(500).json(Map.of(
            "error", "Internal Server Error",
            "message", e.getMessage() != null ? e.getMessage() : "Unknown error"
        ));
    }
}

// ============================================================================
// 9. EXAMPLE MAIN CLASS WITH DEMO
// ============================================================================
public class Main {
    public static void main(String[] args) {
        LiteCore app = new LiteCore();
        
        // ====================================================================
        // NEW: ENABLE STATIC FILE SERVING
        // ====================================================================
        app.serveStatic("public");
        
        // ====================================================================
        // MIDDLEWARE EXAMPLES
        // ====================================================================
        
        // Logging Middleware
        app.use((req, res) -> {
            System.out.println("📝 [" + req.getMethod() + "] " + req.getPath());
            return true;
        });
        
        // Authentication Middleware Example (only for API routes)
        app.use((req, res) -> {
            String path = req.getPath();
            
            // Skip auth for public routes and static files
            if (path.equals("/") || path.equals("/hello") || 
                path.startsWith("/greet") || path.startsWith("/user/") ||
                path.endsWith(".html") || path.endsWith(".css") || 
                path.endsWith(".js") || path.endsWith(".ico")) {
                return true;
            }
            
            // Only check auth for protected API routes
            if (path.startsWith("/api/")) {
                String authHeader = req.getHeader("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    res.status(401).json(Map.of(
                        "error", "Unauthorized",
                        "message", "Missing or invalid authorization token"
                    ));
                    return false;
                }
            }
            
            return true;
        });
        
        // ====================================================================
        // ROUTE DEFINITIONS (ALL EXISTING ROUTES KEPT)
        // ====================================================================
        
        // Simple GET route
        app.get("/api/info", (req, res) -> {
            res.json(Map.of(
                "message", "Welcome to LiteCore!",
                "version", "1.0.0",
                "framework", "LiteCore - Lightweight Java Backend"
            ));
        });
        
        // Plain text response
        app.get("/hello", (req, res) -> {
            res.text("Hello, World! 🌍");
        });
        
        // Query parameters example - MODIFIED FOR CORS
        app.get("/greet", (req, res) -> {
            String name = req.getQueryParam("name");
            if (name == null) {
                name = "Guest";
            }
            // Add CORS header for frontend access
            res.header("Access-Control-Allow-Origin", "*");
            res.json(Map.of("greeting", "Hello, " + name + "!"));
        });
        
        // Dynamic route with path parameters - MODIFIED FOR CORS
        app.get("/user/:id", (req, res) -> {
            String userId = req.getPathParam("id");
            // Add CORS header for frontend access
            res.header("Access-Control-Allow-Origin", "*");
            res.json(Map.of(
                "userId", userId,
                "name", "User " + userId,
                "email", "user" + userId + "@example.com",
                "role", "Developer",
                "joinedDate", "2024-01-15"
            ));
        });
        
        // POST request with body
        app.post("/users", (req, res) -> {
            String body = req.getBody();
            res.status(201).json(Map.of(
                "message", "User created successfully",
                "receivedData", body
            ));
        });
        
        // Multiple path parameters
        app.get("/posts/:postId/comments/:commentId", (req, res) -> {
            String postId = req.getPathParam("postId");
            String commentId = req.getPathParam("commentId");
            
            res.json(Map.of(
                "postId", postId,
                "commentId", commentId,
                "content", "This is comment " + commentId + " on post " + postId
            ));
        });
        
        // Error handling example
        app.get("/error", (req, res) -> {
            throw new RuntimeException("This is a test error!");
        });
        
        // HTML response example
        app.get("/page", (req, res) -> {
            res.html("<h1>Welcome to LiteCore</h1><p>This is a simple HTML page.</p>");
        });
        
        // CRUD example - simple in-memory data store
        Map<String, Map<String, Object>> users = new ConcurrentHashMap<>();
        
        // CREATE
        app.post("/api/users", (req, res) -> {
            String id = UUID.randomUUID().toString();
            Map<String, Object> user = Map.of(
                "id", id,
                "data", req.getBody(),
                "createdAt", System.currentTimeMillis()
            );
            users.put(id, user);
            res.status(201).json(user);
        });
        
        // READ ALL
        app.get("/api/users", (req, res) -> {
            res.json(new ArrayList<>(users.values()));
        });
        
        // READ ONE
        app.get("/api/users/:id", (req, res) -> {
            String id = req.getPathParam("id");
            Map<String, Object> user = users.get(id);
            if (user != null) {
                res.json(user);
            } else {
                res.status(404).json(Map.of("error", "User not found"));
            }
        });
        
        // UPDATE
        app.put("/api/users/:id", (req, res) -> {
            String id = req.getPathParam("id");
            if (users.containsKey(id)) {
                Map<String, Object> user = new HashMap<>(users.get(id));
                user.put("data", req.getBody());
                user.put("updatedAt", System.currentTimeMillis());
                users.put(id, user);
                res.json(user);
            } else {
                res.status(404).json(Map.of("error", "User not found"));
            }
        });
        
        // DELETE
        app.delete("/api/users/:id", (req, res) -> {
            String id = req.getPathParam("id");
            if (users.remove(id) != null) {
                res.json(Map.of("message", "User deleted successfully"));
            } else {
                res.status(404).json(Map.of("error", "User not found"));
            }
        });
        
        // ====================================================================
        // START SERVER
        // ====================================================================
        
        app.start(8080);
        
        // Print available routes
        System.out.println("\n📋 Backend API Routes:");
        System.out.println("   GET  /api/info");
        System.out.println("   GET  /hello");
        System.out.println("   GET  /greet?name=YourName");
        System.out.println("   GET  /user/:id");
        System.out.println("   POST /users");
        System.out.println("   GET  /posts/:postId/comments/:commentId");
        System.out.println("   GET  /error");
        System.out.println("   GET  /page");
        System.out.println("   POST /api/users");
        System.out.println("   GET  /api/users");
        System.out.println("   GET  /api/users/:id");
        System.out.println("   PUT  /api/users/:id");
        System.out.println("   DELETE /api/users/:id");
        System.out.println("\n🌐 Frontend:");
        System.out.println("   Open http://localhost:8080/ in your browser");
        System.out.println("\n✅ Server ready!");
    }
}