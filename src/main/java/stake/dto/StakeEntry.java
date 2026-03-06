package stake.dto;

/**
 * Entry for sorted set: (customerId, stake). Equality by both fields for correct remove in sets.
 */
public final class StakeEntry {

    private final int customerId;
    private final int stake;

    public StakeEntry(int customerId, int stake) {
        this.customerId = customerId;
        this.stake = stake;
    }

    public int getCustomerId() {
        return customerId;
    }

    public int getStake() {
        return stake;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StakeEntry)) return false;
        StakeEntry that = (StakeEntry) o;
        return customerId == that.customerId && stake == that.stake;
    }

    @Override
    public int hashCode() {
        return (31 * customerId + stake) % Integer.MAX_VALUE;
    }
}
