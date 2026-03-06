package stake.dto;

/**
 * Result of a security check: allowed or denied with reason.
 */
public record SecurityResult(boolean allowed, String reason) {

    public static SecurityResult allow() {
        return new SecurityResult(true, null);
    }

    public static SecurityResult deny(String reason) {
        return new SecurityResult(false, reason);
    }
}
