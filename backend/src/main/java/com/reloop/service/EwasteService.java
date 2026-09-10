package com.reloop.service;

import com.reloop.controller.EwasteItemRequest;
import com.reloop.exception.InvalidItemException;
import com.reloop.exception.ResourceNotFoundException;
import com.reloop.model.Accessory;
import com.reloop.model.Battery;
import com.reloop.model.Condition;
import com.reloop.model.EwasteItem;
import com.reloop.model.Laptop;
import com.reloop.model.MobilePhone;
import com.reloop.model.User;
import com.reloop.repository.EwasteItemRepository;
import com.reloop.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Business logic for registering and reading e-waste items. Controllers
 * never construct entities or call the repository directly - they go
 * through here.
 */
@Service
public class EwasteService {

    private final EwasteItemRepository itemRepository;
    private final UserRepository userRepository;

    public EwasteService(EwasteItemRepository itemRepository, UserRepository userRepository) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    /**
     * FACTORY-STYLE CREATION + POLYMORPHISM: based on the request's
     * itemType string, we instantiate the matching subclass. From this
     * point on, every other method in the app (reward calculation,
     * priority ordering, persistence) works through the abstract
     * EwasteItem reference and never needs another type check.
     */
    public EwasteItem registerItem(EwasteItemRequest request) {
        Condition condition = parseCondition(request.getCondition());
        String location = requireNonBlank(request.getLocation(), "Location is required");
        // setWeightKg() (called by each subclass constructor via super()) already rejects
        // weight <= 0, so no separate weight check is needed here.
        EwasteItem item = switch (request.getItemType() == null ? "" : request.getItemType().toUpperCase()) {
            case "LAPTOP" -> new Laptop(request.getWeightKg(), condition, location, request.getDescription());
            case "MOBILE_PHONE" -> new MobilePhone(request.getWeightKg(), condition, location, request.getDescription());
            case "BATTERY" -> new Battery(request.getWeightKg(), condition, location, request.getDescription());
            case "ACCESSORY" -> new Accessory(request.getWeightKg(), condition, location, request.getDescription());
            default -> throw new InvalidItemException("Unknown item type: " + request.getItemType());
        };
        if (request.getUserId() != null) {
            item.setOwner(findUser(request.getUserId()));
        }
        return itemRepository.save(item);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private Condition parseCondition(String raw) {
        try {
            return Condition.valueOf(raw == null ? "" : raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidItemException("Unknown condition: " + raw);
        }
    }

    private String requireNonBlank(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new InvalidItemException(errorMessage);
        }
        return value;
    }

    public List<EwasteItem> getAllItems() {
        return itemRepository.findAll();
    }

    /** Backs "My Items" - a citizen sees only the devices they registered. */
    public List<EwasteItem> getItemsByOwner(Long ownerId) {
        return itemRepository.findByOwner_Id(ownerId);
    }

    public EwasteItem getItemById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + id));
    }

    /**
     * Recent activity feed for the dashboard. Built as an ArrayList because
     * we only ever need it in insertion order for sequential display - no
     * key lookup, so a List is the right structure (not a Map), and
     * ArrayList over LinkedList since we only append and never insert in
     * the middle.
     */
    public List<EwasteItem> getRecentItems(int limit) {
        List<EwasteItem> all = itemRepository.findAll();
        List<EwasteItem> recent = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0 && recent.size() < limit; i--) {
            recent.add(all.get(i));
        }
        return recent;
    }
}
