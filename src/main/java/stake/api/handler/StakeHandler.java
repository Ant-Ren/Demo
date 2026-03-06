package stake.api.handler;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import stake.api.HttpUtils;
import stake.service.SessionService;
import stake.service.StakeService;

/**
 * Handles all stake-related endpoints:
 * - POST /<betofferid>/stake?sessionkey=<key> — submit a stake (body = amount)
 * - GET /<betofferid>/highstakes — return top N stakes as CSV
 */
public final class StakeHandler implements HttpHandler {

    private final SessionService sessionService;
    private final StakeService stakeService;
    private final int topStakes;
    private final HttpUtils http;

    public StakeHandler(SessionService sessionService, StakeService stakeService, int topStakes, HttpUtils http) {
        this.sessionService = sessionService;
        this.stakeService = stakeService;
        this.topStakes = topStakes;
        this.http = http;
    }

    /**
     * Dispatches by method and path:
     * POST /<betofferid>/stake -> add stake;
     * GET /<betofferid>/highstakes -> get top N.
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String[] segments = http.pathSegments(exchange);
        if (segments.length < 2) {
            http.sendResponse(exchange, 400, "Bad Request: invalid path");
            return;
        }
        String resource = segments[1];

        int betOfferId = http.parseId(segments[0]);
        if (betOfferId < 1) {
            http.sendResponse(exchange, 400, "Bad Request: invalid bet offer ID");
            return;
        }

        if ("POST".equals(method) && "stake".equals(resource)) {
            handlePostStake(exchange, betOfferId);
            return;
        }
        if ("GET".equals(method) && "highstakes".equals(resource)) {
            handleGetHighStakes(exchange, betOfferId);
            return;
        }
        http.sendResponse(exchange, 404, "Endpoint Not Found");
    }

    private void handlePostStake(HttpExchange exchange, int betOfferId) throws IOException {
        String sessionKey = http.getQueryParam(exchange, "sessionkey");
        if (sessionKey == null || sessionKey.isEmpty()) {
            http.sendResponse(exchange, 400, "Bad Request: invalid session key");
            return;
        }

        Integer customerId = sessionService.getCustomerIdBySessionKey(sessionKey);
        if (customerId == null) {
            http.sendResponse(exchange, 401, "Unauthorized");
            return;
        }

        String body = http.readRequestBody(exchange);
        int stake = http.parseStake(body);
        if (stake < 0) {
            http.sendResponse(exchange, 400, "Bad Request: invalid stake");
            return;
        }
        if (stake == 0) {
            http.sendResponse(exchange, 400, "Bad Request: 0 stake doesn't make sense, please submit a valid stake");
            return;
        }

        stakeService.addStake(betOfferId, customerId, stake);
        http.sendResponse(exchange, 200, "");
    }

    private void handleGetHighStakes(HttpExchange exchange, int betOfferId) throws IOException {
        String csv = stakeService.getHighStakes(betOfferId, topStakes);
        http.sendResponse(exchange, 200, csv);
    }
}
