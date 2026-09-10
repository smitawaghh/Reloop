package com.reloop.controller;

import com.reloop.model.RecyclerCenter;
import com.reloop.repository.RecyclerCenterRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Minimal list/create so the Pickups page can offer a real dropdown of centers to assign. */
@RestController
@RequestMapping("/api/recycler-centers")
public class RecyclerCenterController {

    private final RecyclerCenterRepository recyclerCenterRepository;

    public RecyclerCenterController(RecyclerCenterRepository recyclerCenterRepository) {
        this.recyclerCenterRepository = recyclerCenterRepository;
    }

    @GetMapping
    public List<RecyclerCenter> getAllRecyclerCenters() {
        return recyclerCenterRepository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecyclerCenter createRecyclerCenter(@RequestBody RecyclerCenter recyclerCenter) {
        return recyclerCenterRepository.save(recyclerCenter);
    }
}
