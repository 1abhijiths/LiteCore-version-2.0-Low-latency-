import java.util.HashMap;
import java.util.Map;

public class Request {
    public String method;
    public String path;
    public Map<String, String> headers = new HashMap<>();
    public Map<String, String> params = new HashMap<>();
    public Map<String, String> query = new HashMap<>();
    public String body = "";

    // NEW FIELD FOR TOKEN AUTH
    public String user = null;

    public Request(String method, String path) {
        this.method = method;
        this.path = path;

        // Parse query string
        if (path.contains("?")) {
            String[] parts = path.split("\\?", 2);
            this.path = parts[0];
            parseQuery(parts[1]);
        }
    }

    private void parseQuery(String q) {
        String[] pairs = q.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) query.put(kv[0], kv[1]);
        }
    }
}
