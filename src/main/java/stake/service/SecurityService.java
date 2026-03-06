package stake.service;

import com.sun.net.httpserver.HttpExchange;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import stake.dto.SecurityResult;
import stake.dto.UnsafeEvent;

/**
 * Checks incoming requests for security issues: path, headers, query.
 * Records client IP and logs unsafe events.
 */
public final class SecurityService {

    private static final SecurityService INSTANCE = new SecurityService();
    private static final int MAX_QUERY_LENGTH = 2048;
    private static final int MAX_HEADER_VALUE_LENGTH = 4096;
    private static final int MAX_HEADER_NAME_LENGTH = 256;
    private static final String[] SUSPICIOUS_HEADER_NAMES = {
        "..", "\r", "\n", "<", ">", "script", "javascript", "vbscript", "onload", "onerror"
    };

    private final List<UnsafeEvent> unsafeEvents = new CopyOnWriteArrayList<>();
    private static final int MAX_EVENTS = 1000;

    private SecurityService() {}

    public static SecurityService getInstance() {
        return INSTANCE;
    }

    public SecurityResult check(HttpExchange exchange) {
        String clientIp = getClientIp(exchange);

        String rawPath = getRawPath(exchange);
        String authority = exchange.getRequestURI().getAuthority();
        if (authority != null || (rawPath != null && rawPath.startsWith("//"))) {
            recordUnsafe(clientIp, "PATH_UNSAFE", "raw path starts with //: " + rawPath);
            return SecurityResult.deny("Invalid path");
        }

        String headerIssue = checkHeaders(exchange);
        if (headerIssue != null) {
            recordUnsafe(clientIp, "HEADER_UNSAFE", headerIssue);
            return SecurityResult.deny("Invalid request headers");
        }

        String queryIssue = checkQuery(exchange);
        if (queryIssue != null) {
            recordUnsafe(clientIp, "QUERY_UNSAFE", queryIssue);
            return SecurityResult.deny("Invalid query parameters");
        }

        return SecurityResult.allow();
    }

    private static String getClientIp(HttpExchange exchange) {
        if (exchange.getRemoteAddress() != null) {
            return exchange.getRemoteAddress().getAddress().getHostAddress();
        }
        return "Unknown";
    }

    private static String getRawPath(HttpExchange exchange) {
        URI uri = exchange.getRequestURI();
        String raw = uri.getRawPath();
        return raw != null ? raw : uri.getPath();
    }

    private String checkHeaders(HttpExchange exchange) {
        for (Map.Entry<String, List<String>> entry : exchange.getRequestHeaders().entrySet()) {
            String name = entry.getKey();
            if (name != null && name.length() > MAX_HEADER_NAME_LENGTH) {
                return "Header name too long: " + name.length();
            }
            String nameLower = name == null ? "" : name.toLowerCase();
            for (String bad : SUSPICIOUS_HEADER_NAMES) {
                if (nameLower.contains(bad)) {
                    return "Suspicious header name: " + name;
                }
            }
            for (String value : entry.getValue()) {
                if (value != null && value.length() > MAX_HEADER_VALUE_LENGTH) {
                    return "Header value too long: " + name;
                }
                if (containsSuspiciousChars(value)) {
                    return "Suspicious characters in header: " + name;
                }
            }
        }
        return null;
    }

    private static boolean containsSuspiciousChars(String s) {
        if (s == null) return false;
        return s.indexOf('\r') >= 0 || s.indexOf('\n') >= 0
            || s.indexOf('<') >= 0 && s.indexOf('>') >= 0
            || s.toLowerCase().contains("script") || s.toLowerCase().contains("javascript");
    }

    private String checkQuery(HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null) return null;
        if (query.length() > MAX_QUERY_LENGTH) {
            return "Query too long: " + query.length();
        }
        if (containsSuspiciousChars(query)
            || query.contains("..")
            || query.contains("%00")
            || query.contains("\0")) {
            return "Suspicious query content";
        }
        return null;
    }

    private void recordUnsafe(String clientIp, String kind, String detail) {
        UnsafeEvent event = new UnsafeEvent(clientIp, kind, detail);
        unsafeEvents.add(event);
        while (unsafeEvents.size() > MAX_EVENTS) {
            unsafeEvents.remove(0);
        }
        System.err.println("[SecurityService] " + event);
    }

    public List<UnsafeEvent> getUnsafeEvents() {
        return new ArrayList<>(unsafeEvents);
    }
}
