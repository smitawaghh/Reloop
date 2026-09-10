package com.reloop.service;

import com.reloop.model.EwasteItem;
import com.reloop.model.Pickup;
import com.reloop.model.PickupStatus;
import com.reloop.repository.EwasteItemRepository;
import com.reloop.repository.PickupRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Aggregates estimated environmental/impact numbers for the Dashboard and
 * Impact pages. These are ESTIMATES derived from registered weight and
 * reward data, not measured environmental science.
 */
@Service
public class ImpactService {

    private final EwasteItemRepository itemRepository;
    private final PickupRepository pickupRepository;

    public ImpactService(EwasteItemRepository itemRepository, PickupRepository pickupRepository) {
        this.itemRepository = itemRepository;
        this.pickupRepository = pickupRepository;
    }

    public Map<String, Object> getStats() {
        List<EwasteItem> items = itemRepository.findAll();
        List<Pickup> pickups = pickupRepository.findAll();

        double totalWeightKg = 0;
        double totalEcoPoints = 0;

        /**
         * HASHMAP: we tally weight per device type for the impact
         * breakdown. Item type strings are the natural key here and we
         * only ever need "add this item's weight to its type's running
         * total", which is an O(1) average-case get+put per item - a
         * HashMap is the direct fit, no ordering requirement that would
         * justify anything else (e.g. a TreeMap).
         */
        Map<String, Double> weightByType = new HashMap<>();

        for (EwasteItem item : items) {
            totalWeightKg += item.getWeightKg();
            totalEcoPoints += item.calculateReward();
            weightByType.merge(item.getItemType(), item.getWeightKg(), Double::sum);
        }

        long recycledCount = pickups.stream()
                .filter(p -> p.getStatus() == PickupStatus.RECYCLED)
                .count();
        long activePickups = pickups.stream()
                .filter(p -> p.getStatus() != PickupStatus.RECYCLED)
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalItemsRegistered", items.size());
        stats.put("activePickups", activePickups);
        stats.put("recycledCount", recycledCount);
        stats.put("totalEcoPoints", totalEcoPoints);
        stats.put("estimatedEwasteDivertedKg", totalWeightKg);
        stats.put("weightByTypeKg", weightByType);
        return stats;
    }
}
