package stake;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Encapsulates all request handling and response logic for the stake API:
 * - GET /<customerid>/session
 * - POST /<betofferid>/stake
 * - GET /<betofferid>/highstakes
 */
final class RequestHandler {

    private final SessionStore sessionStore;
    private final StakeStore stakeStore;
    private final int topStakes;

    public RequestHandler(SessionStore sessionStore, StakeStore stakeStore, int topStakes) {
        this.sessionStore = sessionStore;
        this.stakeStore = stakeStore;
        this.topStakes = topStakes;
    }

    /**
     * Handles the exchange: route by method and path, then invoke the appropriate handler.
     */
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        path = path.endsWith("/") && path.length() > 0 ? path.substring(0, path.length() - 1) : path;

        if (path.isEmpty()) {
            sendResponse(exchange, 200, "Welcome to the BET-OFFER STAKE system!");
            return;
        }

        path = path.startsWith("/") && path.length() > 0 ? path.substring(1) : path;
        String[] paths = path.split("/");
        // Log request paths
        System.out.println("Request paths: " + Arrays.toString(paths));

        if ("GET".equals(method) && paths.length == 2 && "session".equals(paths[1])) {
            handleGetSession(exchange, paths[0]);
            return;
        }
        if ("GET".equals(method) && paths.length == 2 && "highstakes".equals(paths[1])) {
            handleGetHighStakes(exchange, paths[0]);
            return;
        }
        if ("POST".equals(method) && paths.length == 2 && "stake".equals(paths[1])) {
            handlePostStake(exchange, paths[0]);
            return;
        }
        sendResponse(exchange, 404, "Endpoint Not Found");
    }

    public void handleGetSession(HttpExchange exchange, String customerIdStr) throws IOException {
        int customerId = parseId(customerIdStr);
        if (customerId < 0) {
            sendResponse(exchange, 400, "Bad Request: invalid customer ID");
            return;
        }
        String sessionKey = sessionStore.getOrCreateSession(customerId);
        sendResponse(exchange, 200, sessionKey);
    }

    public void handlePostStake(HttpExchange exchange, String betOfferIdStr) throws IOException {
        int betOfferId = parseId(betOfferIdStr);
        if (betOfferId < 0) {
            sendResponse(exchange, 400, "Bad Request: invalid bet offer ID");
            return;
        }
        String sessionKey = getQueryParam(exchange.getRequestURI().getQuery(), "sessionkey");
        if (sessionKey == null || sessionKey.isEmpty()) {
            sendResponse(exchange, 400, "Bad Request: invalid session key");
            return;
        }
        Integer customerId = sessionStore.getCustomerIdBySessionKey(sessionKey);
        if (customerId == null) {
            sendResponse(exchange, 401, "Unauthorized");
            return;
        }
        String body = readRequestBody(exchange);
        int stake = parseStake(body);
        if (stake < 0) {
            sendResponse(exchange, 400, "Bad Request: invalid stake");
            return;
        } else if (stake == 0) {
            sendResponse(exchange, 400, "Bad Request: 0 stake doesn't make sense, please submit a valid stake");
            return;
        }
        stakeStore.addStake(betOfferId, customerId, stake);
        sendResponse(exchange, 200, "");
    }

    public void handleGetHighStakes(HttpExchange exchange, String betOfferIdStr) throws IOException {
        int betOfferId = parseId(betOfferIdStr);
        if (betOfferId < 0) {
            sendResponse(exchange, 400, "Bad Request: invalid bet offer ID");
            return;
        }
        String csv = stakeStore.getHighStakesCsv(betOfferId, topStakes);
        sendResponse(exchange, 200, csv);
    }

    public void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        if (bytes.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        } else {
            exchange.getResponseBody().close();
        }
    }

    private static int parseId(String s) {
        if (s == null || s.isEmpty()) return -1;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int parseStake(String body) {
        if (body == null) return -1;
        body = body.trim();
        if (body.isEmpty()) return -1;
        try {
            return Integer.parseInt(body);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String getQueryParam(String query, String name) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && name.equals(pair.substring(0, eq).trim())) {
                String value = pair.substring(eq + 1).trim();
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
