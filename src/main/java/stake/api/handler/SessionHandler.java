package stake.api.handler;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import stake.api.HttpUtils;
import stake.service.SessionService;

/**
 * Handles GET /&lt;customerId&gt;/session — get or create session for customer.
 */
public final class SessionHandler implements HttpHandler {

    private final SessionService sessionService;
    private final HttpUtils http;

    public SessionHandler(SessionService sessionService, HttpUtils http) {
        this.sessionService = sessionService;
        this.http = http;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String[] segments = http.pathSegments(exchange);
        if (segments.length < 1) {
            http.sendResponse(exchange, 400, "Bad Request: missing customer ID");
            return;
        }
        int customerId = http.parseId(segments[0]);
        if (customerId < 1) {
            http.sendResponse(exchange, 400, "Bad Request: invalid customer ID");
            return;
        }
        String sessionKey = sessionService.getOrCreateSession(customerId);
        http.sendResponse(exchange, 200, sessionKey);
    }
}
