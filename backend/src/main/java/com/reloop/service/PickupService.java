package com.reloop.service;

import com.reloop.exception.ActivePickupExistsException;
import com.reloop.exception.InvalidItemException;
import com.reloop.exception.InvalidPickupStatusTransitionException;
import com.reloop.exception.ResourceNotFoundException;
import com.reloop.model.EwasteItem;
import com.reloop.model.Pickup;
import com.reloop.model.PickupStatus;
import com.reloop.model.RecyclerCenter;
import com.reloop.model.User;
import com.reloop.repository.PickupRepository;
import com.reloop.repository.RecyclerCenterRepository;
import com.reloop.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.springframework.stereotype.Service;

@Service
public class PickupService {

    /**
     * The one legal "next status" for each current status. Anything not
     * listed here (skipping a step, or moving backwards) is rejected.
     * RECYCLED has no entry - it's terminal.
     */
    private static final Map<PickupStatus, PickupStatus> VALID_NEXT_STATUS = new EnumMap<>(PickupStatus.class);

    static {
        VALID_NEXT_STATUS.put(PickupStatus.PENDING, PickupStatus.ASSIGNED);
        VALID_NEXT_STATUS.put(PickupStatus.ASSIGNED, PickupStatus.SCHEDULED);
        VALID_NEXT_STATUS.put(PickupStatus.SCHEDULED, PickupStatus.COLLECTED);
        VALID_NEXT_STATUS.put(PickupStatus.COLLECTED, PickupStatus.RECYCLED);
    }

    private final PickupRepository pickupRepository;
    private final EwasteService ewasteService;
    private final UserRepository userRepository;
    private final RecyclerCenterRepository recyclerCenterRepository;

    public PickupService(
            PickupRepository pickupRepository,
            EwasteService ewasteService,
            UserRepository userRepository,
            RecyclerCenterRepository recyclerCenterRepository) {
        this.pickupRepository = pickupRepository;
        this.ewasteService = ewasteService;
        this.userRepository = userRepository;
        this.recyclerCenterRepository = recyclerCenterRepository;
    }

    public Pickup requestPickup(Long itemId, String pickupLocation, Long userId) {
        if (pickupLocation == null || pickupLocation.isBlank()) {
            throw new InvalidItemException("Pickup location is required");
        }
        EwasteItem item = ewasteService.getItemById(itemId);
        // An item's pickup lifecycle is meant to be linear (PENDING -> ... -> RECYCLED).
        // Allowing a second in-flight pickup for the same item would let two recyclers
        // both think they're responsible for the same physical device.
        if (pickupRepository.existsByItem_IdAndStatusNot(itemId, PickupStatus.RECYCLED)) {
            throw new ActivePickupExistsException("Item " + itemId + " already has an active pickup request");
        }
        Pickup pickup = new Pickup(item, pickupLocation);
        if (userId != null) {
            pickup.setRequestedBy(findUser(userId));
        }
        return pickupRepository.save(pickup);
    }

    public Pickup getPickupById(Long id) {
        return pickupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found: " + id));
    }

    public List<Pickup> getAllPickups() {
        return pickupRepository.findAll();
    }

    /** Backs a citizen's "My Pickups" view. */
    public List<Pickup> getPickupsByRequester(Long userId) {
        return pickupRepository.findByRequestedBy_Id(userId);
    }

    /** PENDING -> ASSIGNED, and records which recycler center takes it. */
    public Pickup assignRecycler(Long pickupId, Long recyclerCenterId) {
        Pickup pickup = getPickupById(pickupId);
        validateTransition(pickup.getStatus(), PickupStatus.ASSIGNED);
        RecyclerCenter center = recyclerCenterRepository.findById(recyclerCenterId)
                .orElseThrow(() -> new ResourceNotFoundException("Recycler center not found: " + recyclerCenterId));
        pickup.setRecyclerCenter(center);
        pickup.setStatus(PickupStatus.ASSIGNED);
        return pickupRepository.save(pickup);
    }

    /** ASSIGNED -> SCHEDULED, recording when the pickup will happen. */
    public Pickup scheduleDatetime(Long pickupId, LocalDate pickupDate, LocalTime pickupTime) {
        if (pickupDate == null || pickupTime == null) {
            throw new InvalidItemException("Pickup date and time are both required to schedule a pickup");
        }
        Pickup pickup = getPickupById(pickupId);
        validateTransition(pickup.getStatus(), PickupStatus.SCHEDULED);
        pickup.setPickupDate(pickupDate);
        pickup.setPickupTime(pickupTime);
        pickup.setStatus(PickupStatus.SCHEDULED);
        return pickupRepository.save(pickup);
    }

    /**
     * Pickup is the single owner of pickup lifecycle state. EwasteItem no
     * longer carries its own status field, so there's nothing else to keep
     * in sync here - update the Pickup and we're done.
     *
     * Used for the two transitions that don't need extra data attached
     * (SCHEDULED -> COLLECTED, COLLECTED -> RECYCLED); ASSIGNED and
     * SCHEDULED have their own dedicated methods above because they each
     * require extra fields (a recycler center, a date/time).
     */
    public Pickup updateStatus(Long pickupId, PickupStatus status) {
        Pickup pickup = getPickupById(pickupId);
        validateTransition(pickup.getStatus(), status);
        pickup.setStatus(status);
        return pickupRepository.save(pickup);
    }

    /**
     * The pickup lifecycle is meant to move forward one step at a time:
     * PENDING -> ASSIGNED -> SCHEDULED -> COLLECTED -> RECYCLED. Skipping a
     * step (PENDING -> COLLECTED) or moving backwards (SCHEDULED ->
     * ASSIGNED) would leave the recycler-side workflow in an inconsistent
     * state, so every status change - regardless of which method triggers
     * it - is checked against the same single source of truth.
     */
    private void validateTransition(PickupStatus current, PickupStatus target) {
        PickupStatus allowedNext = VALID_NEXT_STATUS.get(current);
        if (allowedNext != target) {
            throw new InvalidPickupStatusTransitionException(
                    "Cannot move a pickup from " + current + " to " + target);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    /**
     * PriorityQueue: models "the next pickup that should be worked" as a
     * min-heap on priorityWeight (Battery=1 ... Accessory=4), which is a
     * closer match to the problem ("give me pending work in hazard order")
     * than sorting a list would be.
     *
     * Honest complexity note: this method drains the queue completely to
     * build the full ordered response the recycler UI needs, which costs
     * O(n log n) - the same asymptotic cost as Collections.sort(pending,
     * comparator) would. Building then fully draining a PriorityQueue is
     * NOT asymptotically faster than sorting when the whole ordering is
     * required; it only wins over sorting when you repeatedly need just
     * the single next-highest-priority element (peek/poll is O(log n),
     * far cheaper than re-sorting the whole list after every pickup), or
     * when pickups arrive incrementally over time rather than all at once.
     * We still use PriorityQueue here because it makes the "highest
     * priority next" intent explicit in the code, and it would let a
     * future incremental/streaming version of this method (poll one pickup
     * at a time as recyclers become available) reuse the same heap without
     * a rewrite.
     */
    public List<Pickup> getPendingPickupsByPriority() {
        List<Pickup> pending = pickupRepository.findByStatus(PickupStatus.PENDING);

        PriorityQueue<Pickup> queue = new PriorityQueue<>(
                Comparator.comparingInt(Pickup::getPriorityWeight));
        queue.addAll(pending);

        List<Pickup> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            ordered.add(queue.poll());
        }
        return ordered;
    }
}
