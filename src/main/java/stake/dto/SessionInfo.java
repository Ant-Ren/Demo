package stake.dto;

/**
 * Session info: customerId and expiry. The sessionKey is the map key, not stored here.
 */
public final class SessionInfo {

    private final int customerId;
    private final long expiryTimeMillis;

    public SessionInfo(int customerId, long expiryTimeMillis) {
        this.customerId = customerId;
        this.expiryTimeMillis = expiryTimeMillis;
    }

    public int getCustomerId() {
        return customerId;
    }

    public long getExpiryTimeMillis() {
        return expiryTimeMillis;
    }

    public boolean isValid() {
        return System.currentTimeMillis() < expiryTimeMillis;
    }
}
