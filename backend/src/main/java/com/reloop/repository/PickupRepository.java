package com.reloop.repository;

import com.reloop.model.Pickup;
import com.reloop.model.PickupStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PickupRepository extends JpaRepository<Pickup, Long> {

    /**
     * Spring Data derives the query from the method name alone - no SQL or
     * @Query annotation needed for a filter this simple.
     */
    List<Pickup> findByStatus(PickupStatus status);

    /** Used to block a second pickup request for an item that already has one in flight. */
    boolean existsByItem_IdAndStatusNot(Long itemId, PickupStatus status);

    /** Backs a citizen's "My Pickups" view. */
    List<Pickup> findByRequestedBy_Id(Long userId);
}
