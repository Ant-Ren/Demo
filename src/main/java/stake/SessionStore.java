package stake;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session info: sessionKey and expiry timestamp.
 */
final class SessionInfo {
    final String sessionKey;
    final long expiryTimeMillis;

    SessionInfo(String sessionKey, long expiryTimeMillis) {
        this.sessionKey = sessionKey;
        this.expiryTimeMillis = expiryTimeMillis;
    }

    boolean isValid() {
        return System.currentTimeMillis() < expiryTimeMillis;
    }
}

/**
 * Session store: get or create a 10-minute valid session by customer ID.
 * Also validates sessionKey and looks up customerId for POST stake.
 * Singleton: only one instance exists globally.
 */
final class SessionStore {
    private static final SessionStore INSTANCE = new SessionStore();
    private static final long SESSION_TTL_MS = 10 * 60 * 1000; // 10 minutes
    private static final int SESSION_KEY_LENGTH = 16;

    private final ConcurrentHashMap<Integer, SessionInfo> byCustomer = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> keyToCustomer = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private SessionStore() {}

    static SessionStore getInstance() {
        return INSTANCE;
    }

    String getOrCreateSession(int customerId) {
        SessionInfo existing = byCustomer.get(customerId);
        if (existing != null && existing.isValid()) {
            return existing.sessionKey;
        }
        String key = generateSessionKey();
        long expiry = System.currentTimeMillis() + SESSION_TTL_MS;
        SessionInfo info = new SessionInfo(key, expiry);
        byCustomer.put(customerId, info);
        keyToCustomer.put(key, customerId);
        return key;
    }

    Integer getCustomerIdBySessionKey(String sessionKey) {
        Integer customerId = keyToCustomer.get(sessionKey);
        if (customerId == null) return null;
        SessionInfo info = byCustomer.get(customerId);
        if (info == null || !info.sessionKey.equals(sessionKey) || !info.isValid()) {
            return null;
        }
        return customerId;
    }

    private String generateSessionKey() {
        byte[] token = new byte[SESSION_KEY_LENGTH];
        random.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }
}
