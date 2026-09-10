package com.reloop.controller;

import com.reloop.model.EwasteItem;
import com.reloop.service.EwasteService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
public class EwasteController {

    private final EwasteService ewasteService;

    public EwasteController(EwasteService ewasteService) {
        this.ewasteService = ewasteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EwasteItem registerItem(@RequestBody EwasteItemRequest request) {
        return ewasteService.registerItem(request);
    }

    @GetMapping
    public List<EwasteItem> getAllItems(@RequestParam(required = false) Long userId) {
        return userId == null ? ewasteService.getAllItems() : ewasteService.getItemsByOwner(userId);
    }

    @GetMapping("/{id}")
    public EwasteItem getItem(@PathVariable Long id) {
        return ewasteService.getItemById(id);
    }

    @GetMapping("/recent")
    public List<EwasteItem> getRecentItems(@RequestParam(defaultValue = "5") int limit) {
        return ewasteService.getRecentItems(limit);
    }
}
