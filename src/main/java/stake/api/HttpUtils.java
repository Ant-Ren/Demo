package stake.api;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Shared HTTP utilities: path parsing, query/body parsing, response sending.
 * Used by ApiRouter and all endpoint handlers.
 */
public final class HttpUtils {

    /**
     * Returns path segments (no leading/trailing slash). Empty path yields empty array.
     */
    public String[] pathSegments(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        if (path == null) return new String[0];
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.startsWith("/") && path.length() > 1) {
            path = path.substring(1);
        }
        if (path.isEmpty()) return new String[0];
        return path.split("/");
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

    public int parseId(String s) {
        if (s == null || s.isEmpty()) return -1;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public int parseStake(String body) {
        if (body == null) return -1;
        body = body.trim();
        if (body.isEmpty()) return -1;
        try {
            return Integer.parseInt(body);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String getQueryParam(HttpExchange exchange, String name) {
        String query = exchange.getRequestURI().getQuery();
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

    public String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
