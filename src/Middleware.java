public interface Middleware {
    void run(Request req, Response res, Runnable next);
}
