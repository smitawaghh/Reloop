package com.reloop.config;

import com.reloop.model.RecyclerCenter;
import com.reloop.model.User;
import com.reloop.model.UserRole;
import com.reloop.repository.RecyclerCenterRepository;
import com.reloop.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Since there's no login, the frontend needs at least one Citizen and one
 * Recycler user to exist so its demo-mode switcher has someone to offer,
 * and at least one RecyclerCenter so the assign dropdown isn't empty on a
 * fresh H2 database. Seeds them once at startup if none exist yet.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RecyclerCenterRepository recyclerCenterRepository;

    public DemoDataSeeder(UserRepository userRepository, RecyclerCenterRepository recyclerCenterRepository) {
        this.userRepository = userRepository;
        this.recyclerCenterRepository = recyclerCenterRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(new User("Demo Citizen", "citizen@example.com", UserRole.CITIZEN));
            userRepository.save(new User("Demo Recycler", "recycler@example.com", UserRole.RECYCLER));
        }
        if (recyclerCenterRepository.count() == 0) {
            recyclerCenterRepository.save(new RecyclerCenter("GreenRecycle Center", "College Hostel"));
            recyclerCenterRepository.save(new RecyclerCenter("EcoWaste Hub", "City Center"));
        }
    }
}
