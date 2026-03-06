package stake.service;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import stake.dto.StakeEntry;

/**
 * Stake service: per-customer max stake per betting offer.
 * Multiple submissions from the same customer are not summed; only the highest stake is kept.
 * Each offer maintains a sorted set (by stake desc) so top-N retrieval is O(topN) per request.
 */
public final class StakeService {

    private static final Comparator<StakeEntry> STAKE_DESC =
        Comparator.comparingInt(StakeEntry::getStake).reversed().thenComparingInt(StakeEntry::getCustomerId);
    private final ConcurrentHashMap<Integer, ConcurrentSkipListSet<StakeEntry>> offerToByStake = new ConcurrentHashMap<>();

    public void addStake(int betOfferId, int customerId, int stake) {
        System.out.println("Adding stake: " + betOfferId + " for customer " + customerId + " with stake " + stake);
        ConcurrentSkipListSet<StakeEntry> byStake = offerToByStake.computeIfAbsent(betOfferId, k -> new ConcurrentSkipListSet<>(STAKE_DESC));
        synchronized (byStake) {
            StakeEntry oldEntry = null;
            for (StakeEntry e : byStake) {
                if (e.getCustomerId() == customerId) {
                    oldEntry = e;
                    break;
                }
            }
            int newStake = (oldEntry == null) ? stake : Math.max(oldEntry.getStake(), stake);
            if (oldEntry != null && oldEntry.getStake() == newStake) {
                return;
            }
            if (oldEntry != null) {
                byStake.remove(oldEntry);
            }
            byStake.add(new StakeEntry(customerId, newStake));
        }
    }

    /**
     * Returns top N highest stakes for the offer, one per customer.
     * Format: "customerId=stake,customerId=stake,..."
     */
    public String getHighStakes(int betOfferId, int topN) {
        System.out.println("Getting high stakes for offer " + betOfferId + " with top " + topN);
        ConcurrentSkipListSet<StakeEntry> byStake = offerToByStake.get(betOfferId);
        if (byStake == null || byStake.isEmpty()) {
            return "";
        }

        synchronized (byStake) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            int cutoffStake = 0;
            for (StakeEntry e : byStake) {
                if (count < topN) {
                    count++;
                    if (count == topN) cutoffStake = e.getStake();
                    if (count > 1) sb.append(',');
                    sb.append(e.getCustomerId()).append('=').append(e.getStake());
                } else {
                    if (e.getStake() != cutoffStake) break;
                    sb.append(',').append(e.getCustomerId()).append('=').append(e.getStake());
                }
            }
            return sb.toString();
        }
    }
}
