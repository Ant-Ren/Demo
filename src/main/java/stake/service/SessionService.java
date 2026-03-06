package stake.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import stake.dto.SessionInfo;

/**
 * Session service: get or create a 10-minute valid session by customer ID.
 * Single map (sessionKey -> SessionInfo); getOrCreateSession uses per-customer lock stripe.
 */
public final class SessionService {

    private static final SessionService INSTANCE = new SessionService();
    private static final long SESSION_TTL_MS = 10 * 60 * 1000; // 10 minutes
    private static final int SESSION_KEY_LENGTH = 16;
    private static final int LOCK_BUCKET_SIZE = 512;

    private final ConcurrentHashMap<String, SessionInfo> bySessionKey = new ConcurrentHashMap<>();
    private final Object[] lockBuckets = new Object[LOCK_BUCKET_SIZE];
    private final SecureRandom random = new SecureRandom();

    private SessionService() {
        for (int i = 0; i < lockBuckets.length; i++) {
            lockBuckets[i] = new Object();
        }
    }

    public static SessionService getInstance() {
        return INSTANCE;
    }

    public String getOrCreateSession(int customerId) {
        Object lock = lockBuckets[customerId % LOCK_BUCKET_SIZE];
        synchronized (lock) {
            for (Map.Entry<String, SessionInfo> e : bySessionKey.entrySet()) {
                SessionInfo info = e.getValue();
                if (info.getCustomerId() == customerId && info.isValid()) {
                    return e.getKey();
                }
            }
            String key = generateSessionKey();
            long expiry = System.currentTimeMillis() + SESSION_TTL_MS;
            bySessionKey.put(key, new SessionInfo(customerId, expiry));
            return key;
        }
    }

    public Integer getCustomerIdBySessionKey(String sessionKey) {
        SessionInfo info = bySessionKey.get(sessionKey);
        if (info == null || !info.isValid()) {
            return null;
        }
        return info.getCustomerId();
    }

    private String generateSessionKey() {
        byte[] token = new byte[SESSION_KEY_LENGTH];
        random.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }
}
