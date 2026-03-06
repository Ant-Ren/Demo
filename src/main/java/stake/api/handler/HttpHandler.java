package stake.api.handler;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

/**
 * Standard interface for HTTP request handlers.
 * All handlers in this package implement this interface.
 */
public interface HttpHandler {

    /**
     * Handles the given exchange and sends the response.
     *
     * @param exchange the HTTP exchange
     * @throws IOException if an I/O error occurs
     */
    void handle(HttpExchange exchange) throws IOException;
}
