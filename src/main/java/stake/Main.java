package stake;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Entry point. Uses SecurityGate for request validation and RequestHandler for API logic.
 * GET /<customerid>/session
 * POST /<betofferid>/stake?sessionkey=<sessionkey>
 * GET /<betofferid>/highstakes
 */
public final class Main {

    private static final int PORT = 8001;
    private static final int TOP_STAKES = 20;

    private final SecurityGate securityGate = SecurityGate.getInstance();
    private final RequestHandler requestHandler = new RequestHandler(
        SessionStore.getInstance(),
        new StakeStore(),
        TOP_STAKES
    );

    public static void main(String[] args) throws IOException {
        Main app = new Main();
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", app::handle);
        server.setExecutor(null);
        server.start();
        System.out.println("Server listening on http://localhost:" + PORT);
    }

    private void handle(HttpExchange exchange) throws IOException {
        SecurityGate.SecurityResult security = securityGate.check(exchange);
        if (!security.allowed()) {
            try {
                requestHandler.sendResponse(exchange, 403, security.reason() != null ? security.reason() : "Forbidden");
            } catch (IOException e) {
                exchange.close();
            }
            return;
        }

        try {
            requestHandler.handle(exchange);
        } catch (IOException e) {
            exchange.close();
        } catch (RuntimeException e) {
            try {
                requestHandler.sendResponse(exchange, 500, "Internal Server Error");
            } catch (IOException ioException) {
                exchange.close();
            }
        }
    }
}
