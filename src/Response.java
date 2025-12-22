/*import java.io.OutputStream;

public class Response {

    private OutputStream out;

    public Response(OutputStream out) {
        this.out = out;
    }

    public void text(String message) {
        try {
            String response =
                "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: " + message.length() + "\r\n" +
                "\r\n" +
                message;

            out.write(response.getBytes());
           out.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void json(String jsonString) {
        try {
            String response =
                "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: " + jsonString.length() + "\r\n" +
                "\r\n" +
                jsonString;

            out.write(response.getBytes());
             out.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}*/
import java.io.OutputStream;

public class Response {

    private OutputStream out;
    private int statusCode = 200;
    private String statusMessage = "OK";

    public Response(OutputStream out) {
        this.out = out;
    }

    // Allow method chaining: res.status(404).json("...");
    public Response status(int code) {
        this.statusCode = code;

        switch (code) {
            case 200 -> statusMessage = "OK";
            case 201 -> statusMessage = "Created";
            case 204 -> statusMessage = "No Content";
            case 400 -> statusMessage = "Bad Request";
            case 401 -> statusMessage = "Unauthorized";
            case 404 -> statusMessage = "Not Found";
            case 500 -> statusMessage = "Internal Server Error";
            default -> statusMessage = "OK";
        }

        return this;
    }

    private void send(String contentType, String body) {
        try {
            String response =
                "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Headers: *\r\n" +
                "Access-Control-Allow-Methods: *\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;

            out.write(response.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void text(String message) {
        send("text/plain", message);
    }

    public void json(String jsonString) {
        send("application/json", jsonString);
    }
}
