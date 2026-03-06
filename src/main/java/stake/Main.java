package stake;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import stake.api.ApiRouter;
import stake.api.HttpUtils;
import stake.api.handler.SessionHandler;
import stake.api.handler.StakeHandler;
import stake.dto.SecurityResult;
import stake.service.SecurityService;
import stake.service.SessionService;
import stake.service.StakeService;

/**
 * Entry point. Pipeline: security → route → exception handling.
 * GET /<customerid>/session
 * POST /<betofferid>/stake?sessionkey=<sessionkey>
 * GET /<betofferid>/highstakes
 */
public final class Main {

    private static final int PORT = 8001;
    private static final int TOP_STAKES = 20;
    private static final int THREAD_POOL_SIZE = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
    private static final String FORBIDDEN_MSG = "Forbidden";
    private static final String INTERNAL_ERROR_MSG = "Internal Server Error";

    private final SecurityService securityService = SecurityService.getInstance();
    private final HttpUtils httpHelper = new HttpUtils();
    private final ApiRouter apiRouter;

    public Main() {
        SessionService sessionService = SessionService.getInstance();
        StakeService stakeService = new StakeService();
        SessionHandler sessionHandler = new SessionHandler(sessionService, httpHelper);
        StakeHandler stakeHandler = new StakeHandler(sessionService, stakeService, TOP_STAKES, httpHelper);
        this.apiRouter = new ApiRouter(httpHelper, sessionHandler, stakeHandler);
    }

    public static void main(String[] args) throws IOException {
        Main app = new Main();
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", app::handle);
        server.setExecutor(Executors.newFixedThreadPool(THREAD_POOL_SIZE));
        server.start();
        System.out.println("Server listening on http://localhost:" + PORT);
    }

    /**
     * Pipeline: 1) Security → 2) Route and handle → 3) On exception, send 500 or close.
     */
    private void handle(HttpExchange exchange) {
        SecurityResult security = securityService.check(exchange);
        if (!security.allowed()) {
            try {
                String reason = security.reason() != null ? security.reason() : FORBIDDEN_MSG;
                httpHelper.sendResponse(exchange, 403, reason);
            } catch (IOException e) {
                exchange.close();
            }
            return;
        }

        try {
            apiRouter.handle(exchange);
        } catch (IOException e) {
            exchange.close();
        } catch (RuntimeException e) {
            try {
                httpHelper.sendResponse(exchange, 500, INTERNAL_ERROR_MSG);
            } catch (IOException ioException) {
                exchange.close();
            }
        }
    }
}
