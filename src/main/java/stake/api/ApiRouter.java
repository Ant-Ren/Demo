package stake.api;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import stake.api.handler.HttpHandler;

/**
 * Routes requests by method and path to SessionHandler or StakeHandler.
 * Root path returns a welcome message; unknown routes return 404.
 */
public final class ApiRouter {

    private static final String WELCOME = "Welcome to the BET-OFFER STAKE system!";

    private final HttpUtils http;
    private final HttpHandler sessionHandler;
    private final HttpHandler stakeHandler;

    public ApiRouter(HttpUtils http, HttpHandler sessionHandler, HttpHandler stakeHandler) {
        this.http = http;
        this.sessionHandler = sessionHandler;
        this.stakeHandler = stakeHandler;
    }

    /**
     * Dispatches the exchange to the appropriate handler based on method and path.
     */
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String[] segments = http.pathSegments(exchange);

        if (segments.length == 0) {
            http.sendResponse(exchange, 200, WELCOME);
            return;
        }

        if (segments.length == 2) {
            String resource = segments[1];
            if ("GET".equals(method) && "session".equals(resource)) {
                sessionHandler.handle(exchange);
                return;
            }
            if ("GET".equals(method) && "highstakes".equals(resource)
                || "POST".equals(method) && "stake".equals(resource)) {
                stakeHandler.handle(exchange);
                return;
            }
        }

        http.sendResponse(exchange, 404, "Endpoint Not Found");
    }
}
