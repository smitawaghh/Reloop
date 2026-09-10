package com.reloop.repository;

import com.reloop.model.EwasteItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Extending JpaRepository gives us save(), findById(), findAll(), delete()
 * etc. for free - Spring generates the implementation at startup via a
 * dynamic proxy, so we never write SQL or an implementation class here.
 */
public interface EwasteItemRepository extends JpaRepository<EwasteItem, Long> {

    /** Backs "My Items" - a citizen's own registered devices. */
    List<EwasteItem> findByOwner_Id(Long ownerId);
}
