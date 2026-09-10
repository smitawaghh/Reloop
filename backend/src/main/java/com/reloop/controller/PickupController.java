package com.reloop.controller;

import com.reloop.model.Pickup;
import com.reloop.model.PickupStatus;
import com.reloop.service.PickupService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pickups")
public class PickupController {

    private final PickupService pickupService;

    public PickupController(PickupService pickupService) {
        this.pickupService = pickupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pickup requestPickup(@RequestBody PickupRequest request) {
        return pickupService.requestPickup(request.getItemId(), request.getPickupLocation(), request.getUserId());
    }

    @GetMapping
    public List<Pickup> getAllPickups(@RequestParam(required = false) Long userId) {
        return userId == null ? pickupService.getAllPickups() : pickupService.getPickupsByRequester(userId);
    }

    @GetMapping("/{id}")
    public Pickup getPickup(@PathVariable Long id) {
        return pickupService.getPickupById(id);
    }

    /** Recycler management view: pending pickups ordered by hazard priority (PriorityQueue-backed). */
    @GetMapping("/pending")
    public List<Pickup> getPendingByPriority() {
        return pickupService.getPendingPickupsByPriority();
    }

    /** PENDING -> ASSIGNED. */
    @PutMapping("/{id}/assign")
    public Pickup assignRecycler(@PathVariable Long id, @RequestBody AssignRecyclerRequest request) {
        return pickupService.assignRecycler(id, request.getRecyclerCenterId());
    }

    /** ASSIGNED -> SCHEDULED, with the date/time the recycler picked. */
    @PutMapping("/{id}/schedule")
    public Pickup schedule(@PathVariable Long id, @RequestBody SchedulePickupRequest request) {
        return pickupService.scheduleDatetime(id, request.getPickupDate(), request.getPickupTime());
    }

    /** SCHEDULED -> COLLECTED, or COLLECTED -> RECYCLED. Any other transition is rejected by the service layer. */
    @PutMapping("/{id}/status")
    public Pickup updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        PickupStatus status = PickupStatus.valueOf(body.get("status").toUpperCase());
        return pickupService.updateStatus(id, status);
    }
}
