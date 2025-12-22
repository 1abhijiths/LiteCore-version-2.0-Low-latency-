// import java.io.*;
// import java.net.ServerSocket;
// import java.net.Socket;
// import java.util.*;

// public class LiteCore {

//     public interface Middleware {
//         void run(Request req, Response res, Runnable next);
//     }

//     public interface Handler {
//         void handle(Request req, Response res);
//     }

//     private List<Middleware> globalMiddleware = new ArrayList<>();
//     private Map<String, Handler> routes = new HashMap<>();
//     private Map<String, Middleware> protectedRoutes = new HashMap<>();
//     private Map<String, Handler> protectedHandlers = new HashMap<>();

//     // -------- LRU Cache --------
//     private Map<String, Handler> routeCache = new LinkedHashMap<>(10, 0.75f, true) {
//         @Override
//         protected boolean removeEldestEntry(Map.Entry<String, Handler> eldest) {
//             return size() > 10;
//         }
//     };

//     // -------- REGISTER MIDDLEWARE --------
//     public void use(Middleware mw) {
//         globalMiddleware.add(mw);
//     }

//     // -------- NORMAL ROUTES --------
//     public void get(String path, Handler handler) { routes.put("GET:" + path, handler); }
//     public void post(String path, Handler handler) { routes.put("POST:" + path, handler); }
//     public void put(String path, Handler handler) { routes.put("PUT:" + path, handler); }
//     public void delete(String path, Handler handler) { routes.put("DELETE:" + path, handler); }

//     // -------- PROTECTED ROUTES --------
//     public void get(String path, Middleware mw, Handler handler) {
//         protectedRoutes.put("GET:" + path, mw);
//         protectedHandlers.put("GET:" + path, handler);
//     }
//     public void post(String path, Middleware mw, Handler handler) {
//         protectedRoutes.put("POST:" + path, mw);
//         protectedHandlers.put("POST:" + path, handler);
//     }
//     public void put(String path, Middleware mw, Handler handler) {
//         protectedRoutes.put("PUT:" + path, mw);
//         protectedHandlers.put("PUT:" + path, handler);
//     }
//     public void delete(String path, Middleware mw, Handler handler) {
//         protectedRoutes.put("DELETE:" + path, mw);
//         protectedHandlers.put("DELETE:" + path, handler);
//     }

//     // -------- START SERVER --------
//     public void start(int port) {
//         try (ServerSocket server = new ServerSocket(port)) {

//             System.out.println("LiteCore running on http://localhost:" + port);
//             System.out.println("Registered routes:");
//             routes.keySet().forEach(System.out::println);
//             protectedHandlers.keySet().forEach(r -> System.out.println("(protected) " + r));

//             while (true) {
//                 Socket socket = server.accept();
//                 new Thread(() -> {
//                     try { handleClient(socket); }
//                     catch (Exception e) { e.printStackTrace(); }
//                 }).start();
//             }

//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }

//     // -------- HANDLE REQUEST --------
//     private void handleClient(Socket socket) throws Exception {

//         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//         OutputStream out = socket.getOutputStream();

//         String firstLine = in.readLine();
//         if (firstLine == null || firstLine.isEmpty()) return;

//         String[] parts = firstLine.split(" ");
//         String method = parts[0];
//         String path = parts[1];

//         // Normalize path (fix trailing slash)
//         if (path.endsWith("/") && path.length() > 1) {
//             path = path.substring(0, path.length() - 1);
//         }

//         Request req = new Request(method, path);
//         Response res = new Response(out);

//         // -------- READ HEADERS --------
//         String line;
//         int contentLength = 0;

//         while ((line = in.readLine()) != null && !line.isEmpty()) {
//             String[] header = line.split(": ", 2);
//             if (header.length == 2) {
//                 req.headers.put(header[0], header[1]);

//                 if (header[0].equalsIgnoreCase("Content-Length")) {
//                     contentLength = Integer.parseInt(header[1]);
//                 }
//             }
//         }

//         // -------- READ BODY SAFELY --------
//         if (contentLength > 0) {
//             char[] bodyBuffer = new char[contentLength];
//             int read = in.read(bodyBuffer, 0, contentLength);
//             req.body = new String(bodyBuffer, 0, read);
//         }

//         // -------- CORS PRE-FLIGHT (CRITICAL FIX) --------
//         if (method.equals("OPTIONS")) {
//             String response =
//                 "HTTP/1.1 204 No Content\r\n" +
//                 "Access-Control-Allow-Origin: *\r\n" +
//                 "Access-Control-Allow-Headers: *\r\n" +
//                 "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
//                 "\r\n";
//             out.write(response.getBytes());
//             out.flush();
//             socket.close();
//             return;
//         }

//         // -------- ROUTING --------
//         String key = method + ":" + req.path;

//         // Protected route?
//         if (protectedHandlers.containsKey(key)) {

//             Middleware auth = protectedRoutes.get(key);
//             Handler handler = protectedHandlers.get(key);

//             auth.run(req, res, () -> handler.handle(req, res));

//         } else {

//             Handler handler = routes.get(key);

//             if (handler != null) {
//                 executeMW(0, req, res, handler);
//             } else {
//                 res.status(404).json("{\"error\":\"Route not found\"}");
//             }
//         }

//         out.flush();
//         socket.close();
//     }

//     // -------- MIDDLEWARE EXECUTION --------
//     private void executeMW(int index, Request req, Response res, Handler handler) {
//         if (index < globalMiddleware.size()) {
//             Middleware mw = globalMiddleware.get(index);
//             mw.run(req, res, () -> executeMW(index + 1, req, res, handler));
//         } else {
//             handler.handle(req, res);
//         }
//     }
// }

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LiteCore - lightweight Java HTTP framework core
 *
 * Features:
 *  - Global middleware chain (applies to all routes)
 *  - Protected routes with per-route auth middleware
 *  - Thread pool (ExecutorService) for handling connections
 *  - CORS preflight (OPTIONS) handling
 *  - Safe body reading using Content-Length
 *  - Trailing slash normalization
 *  - Simple LRU cache for resolved routes (optional)
 *
 * Expected companion classes:
 *  - Request (fields: method, path, headers map, body, user, etc.)
 *  - Response (constructor Response(OutputStream out), methods: status(int), json(String), text(String))
 *
 * Keep this file minimal and readable so it's easy to extend.
 */
public class LiteCore {

    // Middleware interface: run(req, res, next)
    public interface Middleware {
        void run(Request req, Response res, Runnable next);
    }

    // Handler interface: handle(req, res)
    public interface Handler {
        void handle(Request req, Response res);
    }

    // global middleware list
    private final List<Middleware> globalMiddleware = new ArrayList<>();

    // basic route maps for normal routes
    private final Map<String, Handler> routes = new HashMap<>();

    // protected route storage: auth middleware + handler
    private final Map<String, Middleware> protectedAuth = new HashMap<>();
    private final Map<String, Handler> protectedHandlers = new HashMap<>();

    // LRU cache for hot route lookups (optional small optimization)
    private final Map<String, Handler> routeCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Handler> eldest) {
            return size() > 50;
        }
    };

    // Thread pool for handling connections
    private final ExecutorService threadPool = Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors() * 4));

    // ----- Public API: middleware + route registration -----

    // register global middleware
    public void use(Middleware mw) {
        globalMiddleware.add(mw);
    }

    // normal routes
    public void get(String path, Handler handler)    { routes.put("GET:" + normalize(path), handler); }
    public void post(String path, Handler handler)   { routes.put("POST:" + normalize(path), handler); }
    public void put(String path, Handler handler)    { routes.put("PUT:" + normalize(path), handler); }
    public void delete(String path, Handler handler) { routes.put("DELETE:" + normalize(path), handler); }

    // protected routes (auth middleware provided per-route)
    public void get(String path, Middleware auth, Handler handler)    { registerProtected("GET", path, auth, handler); }
    public void post(String path, Middleware auth, Handler handler)   { registerProtected("POST", path, auth, handler); }
    public void put(String path, Middleware auth, Handler handler)    { registerProtected("PUT", path, auth, handler); }
    public void delete(String path, Middleware auth, Handler handler) { registerProtected("DELETE", path, auth, handler); }

    private void registerProtected(String method, String path, Middleware auth, Handler handler) {
        String key = method + ":" + normalize(path);
        protectedAuth.put(key, auth);
        protectedHandlers.put(key, handler);
    }

    // ----- Server lifecycle -----

    public void start(int port) {
        try (ServerSocket server = new ServerSocket(port)) {

            System.out.println("LiteCore running on http://localhost:" + port);
            System.out.println("Registered routes:");
            routes.keySet().forEach(System.out::println);
            protectedHandlers.keySet().forEach(k -> System.out.println("(protected) " + k));

            while (true) {
                final Socket socket = server.accept();
                threadPool.submit(() -> {
                    try {
                        handleClient(socket);
                    } catch (Exception e) {
                        e.printStackTrace();
                        try { socket.close(); } catch (Exception ex) {}
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----- Request handling -----

    private void handleClient(Socket socket) throws Exception {
        InputStream rawIn = socket.getInputStream();
        BufferedInputStream bin = new BufferedInputStream(rawIn);
        BufferedReader in = new BufferedReader(new InputStreamReader(bin));
        OutputStream out = socket.getOutputStream();

        String firstLine = in.readLine();
        if (firstLine == null || firstLine.isEmpty()) {
            socket.close();
            return;
        }

        String[] parts = firstLine.split(" ");
        if (parts.length < 2) {
            socket.close();
            return;
        }

        String method = parts[0].trim();
        String rawPath = parts[1].trim();
        String path = normalize(rawPath);

        Request req = new Request(method, rawPath); // Request constructor should parse query if implemented
        Response res = new Response(out);

        // Read headers
        String line;
        int contentLength = 0;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            String[] h = line.split(": ", 2);
            if (h.length == 2) {
                req.headers.put(h[0], h[1]);
                if (h[0].equalsIgnoreCase("Content-Length")) {
                    try { contentLength = Integer.parseInt(h[1].trim()); } catch (Exception ex) { contentLength = 0; }
                }
            }
        }

        // Read body if Content-Length present
        if (contentLength > 0) {
            char[] buf = new char[contentLength];
            int read = 0;
            while (read < contentLength) {
                int r = in.read(buf, read, contentLength - read);
                if (r == -1) break;
                read += r;
            }
            req.body = new String(buf, 0, read);
        } else {
            req.body = "";
        }

        // CORS preflight handling (must answer quickly)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            String cors =
                "HTTP/1.1 204 No Content\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Headers: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
                "\r\n";
            out.write(cors.getBytes());
            out.flush();
            socket.close();
            return;
        }

        String key = method + ":" + path;

        // If protected route exists, run: global middleware -> auth -> handler
        if (protectedHandlers.containsKey(key)) {

            Handler finalHandler = protectedHandlers.get(key);
            Middleware auth = protectedAuth.get(key);

            // execute global middleware chain first, then auth, then handler
            executeMW(0, req, res, (rq, rs) -> {
                auth.run(rq, rs, () -> {
                    finalHandler.handle(rq, rs);
                });
            });

        } else {
            // Normal route path
            Handler handler = resolveRouteFromCacheOrMap(key);
            if (handler != null) {
                // run global middleware chain then handler
                executeMW(0, req, res, handler);
            } else {
                res.status(404).json("{\"error\":\"Route not found\"}");
            }
        }

        out.flush();
        socket.close();
    }

    // ----- Helper: execute global middleware chain -----
    private void executeMW(int index, Request req, Response res, Handler finalHandler) {
        if (index < globalMiddleware.size()) {
            Middleware mw = globalMiddleware.get(index);
            mw.run(req, res, () -> executeMW(index + 1, req, res, finalHandler));
        } else {
            finalHandler.handle(req, res);
        }
    }

    // ----- Helper: simple route resolution + caching -----
    private Handler resolveRouteFromCacheOrMap(String key) {
        Handler h = routeCache.get(key);
        if (h != null) return h;
        h = routes.get(key);
        if (h != null) {
            routeCache.put(key, h);
            return h;
        }
        return null;
    }

    // ----- Utility: normalize path (remove trailing slash except root) -----
    private String normalize(String path) {
        if (path == null || path.isEmpty()) return "/";
        // remove query string if present
        int q = path.indexOf('?');
        if (q >= 0) path = path.substring(0, q);
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }
}
