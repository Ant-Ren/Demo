package stake.dto;

/**
 * Record of an unsafe event: client IP and description.
 */
public record UnsafeEvent(String clientIp, String kind, String detail) {}
