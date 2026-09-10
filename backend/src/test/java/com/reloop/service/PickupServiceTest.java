package com.reloop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.reloop.exception.ActivePickupExistsException;
import com.reloop.exception.InvalidItemException;
import com.reloop.exception.InvalidPickupStatusTransitionException;
import com.reloop.exception.ResourceNotFoundException;
import com.reloop.model.Accessory;
import com.reloop.model.Battery;
import com.reloop.model.Condition;
import com.reloop.model.EwasteItem;
import com.reloop.model.Laptop;
import com.reloop.model.MobilePhone;
import com.reloop.model.Pickup;
import com.reloop.model.PickupStatus;
import com.reloop.model.RecyclerCenter;
import com.reloop.model.User;
import com.reloop.model.UserRole;
import com.reloop.repository.PickupRepository;
import com.reloop.repository.RecyclerCenterRepository;
import com.reloop.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PickupServiceTest {

    @Mock
    private PickupRepository pickupRepository;

    @Mock
    private EwasteService ewasteService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecyclerCenterRepository recyclerCenterRepository;

    private PickupService newService() {
        return new PickupService(pickupRepository, ewasteService, userRepository, recyclerCenterRepository);
    }

    @Test
    void requestPickupCreatesAPendingPickupForAnExistingItem() {
        PickupService service = newService();
        EwasteItem item = new Laptop(2.0, Condition.WORKING, "Home", "x");
        item.setId(1L);
        when(ewasteService.getItemById(1L)).thenReturn(item);
        when(pickupRepository.existsByItem_IdAndStatusNot(1L, PickupStatus.RECYCLED)).thenReturn(false);
        when(pickupRepository.save(any(Pickup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pickup pickup = service.requestPickup(1L, "College Hostel", null);

        assertThat(pickup.getStatus()).isEqualTo(PickupStatus.PENDING);
        assertThat(pickup.getItem()).isSameAs(item);
        assertThat(pickup.getPickupLocation()).isEqualTo("College Hostel");
    }

    @Test
    void requestPickupLinksTheRequestingUserWhenProvided() {
        PickupService service = newService();
        EwasteItem item = new Laptop(2.0, Condition.WORKING, "Home", "x");
        item.setId(1L);
        User citizen = new User("Demo Citizen", "citizen@example.com", UserRole.CITIZEN);
        when(ewasteService.getItemById(1L)).thenReturn(item);
        when(pickupRepository.existsByItem_IdAndStatusNot(1L, PickupStatus.RECYCLED)).thenReturn(false);
        when(userRepository.findById(3L)).thenReturn(Optional.of(citizen));
        when(pickupRepository.save(any(Pickup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pickup pickup = service.requestPickup(1L, "College Hostel", 3L);

        assertThat(pickup.getRequestedBy()).isSameAs(citizen);
    }

    @Test
    void requestPickupRejectsBlankPickupLocation() {
        PickupService service = newService();

        assertThatThrownBy(() -> service.requestPickup(1L, " ", null))
                .isInstanceOf(InvalidItemException.class);
    }

    @Test
    void requestPickupRejectsASecondActivePickupForTheSameItem() {
        PickupService service = newService();
        EwasteItem item = new Laptop(2.0, Condition.WORKING, "Home", "x");
        item.setId(1L);
        when(ewasteService.getItemById(1L)).thenReturn(item);
        when(pickupRepository.existsByItem_IdAndStatusNot(1L, PickupStatus.RECYCLED)).thenReturn(true);

        assertThatThrownBy(() -> service.requestPickup(1L, "College Hostel", null))
                .isInstanceOf(ActivePickupExistsException.class);
    }

    @Test
    void assignRecyclerMovesPendingToAssignedAndSetsTheCenter() {
        PickupService service = newService();
        Pickup pickup = new Pickup(new Laptop(2.0, Condition.WORKING, "Home", "x"), "Home");
        RecyclerCenter center = new RecyclerCenter("GreenRecycle Center", "College Hostel");
        when(pickupRepository.findById(1L)).thenReturn(Optional.of(pickup));
        when(recyclerCenterRepository.findById(9L)).thenReturn(Optional.of(center));
        when(pickupRepository.save(any(Pickup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pickup updated = service.assignRecycler(1L, 9L);

        assertThat(updated.getStatus()).isEqualTo(PickupStatus.ASSIGNED);
        assertThat(updated.getRecyclerCenter()).isSameAs(center);
    }

    @Test
    void assignRecyclerThrowsWhenRecyclerCenterDoesNotExist() {
        PickupService service = newService();
        Pickup pickup = new Pickup(new Laptop(2.0, Condition.WORKING, "Home", "x"), "Home");
        when(pickupRepository.findById(1L)).thenReturn(Optional.of(pickup));
        when(recyclerCenterRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignRecycler(1L, 404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assignRecyclerRejectsAnAlreadyAssignedPickup() {
        PickupService service = newService();
        Pickup pickup = new Pickup(new Laptop(2.0, Condition.WORKING, "Home", "x"), "Home");
        pickup.setStatus(PickupStatus.ASSIGNED);
        when(pickupRepository.findById(1L)).thenReturn(Optional.of(pickup));

        assertThatThrownBy(() -> service.assignRecycler(1L, 9L))
                .isInstanceOf(InvalidPickupStatusTransitionException.class);
    }

    @Test
    void scheduleDatetimeMovesAssignedToScheduledAndSetsDateAndTime() {
        PickupService service = newService();
        Pickup pickup = new Pickup(new Laptop(2.0, Condition.WORKING, "Home", "x"), "Home");
        pickup.setStatus(PickupStatus.ASSIGNED);
        when(pickupRepository.findById(1L)).thenReturn(Optional.of(pickup));
        when(pickupRepository.save(any(Pickup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate date = LocalDate.of(2026, 9, 10);
        LocalTime time = LocalTime.of(14, 30);
        Pickup updated = service.scheduleDatetime(1L, date, time);

        assertThat(updated.getStatus()).isEqualTo(PickupStatus.SCHEDULED);
        assertThat(updated.getPickupDate()).isEqualTo(date);
        assertThat(updated.getPickupTime()).isEqualTo(time);
    }

    @Test
    void scheduleDatetimeRejectsMissingDateOrTime() {
        PickupService service = newService();

        assertThatThrownBy(() -> service.scheduleDatetime(1L, null, LocalTime.of(9, 0)))
                .isInstanceOf(InvalidItemException.class);
        assertThatThrownBy(() -> service.scheduleDatetime(1L, LocalDate.now(), null))
                .isInstanceOf(InvalidItemException.class);
    }

    @Test
    void scheduleDatetimeRejectsSchedulingAPendingPickup() {
        PickupService service = newService();
        Pickup pickup = new Pickup(new Laptop(2.0, Condition.WORKING, "Home", "x"), "Home");
        when(pickupRepository.findById(1L)).thenReturn(Optional.of(pickup));

        assertThatThrownBy(() -> service.scheduleDatetime(1L, LocalDate.now(), LocalTime.NOON))
                .isInstanceOf(InvalidPickupStatusTransitionException.class);
    }

    @Test
    void updateStatusAllowsEachValidForwardTransition() {
        PickupService service = newService();
        when(pickupRepository.save(any(Pickup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pickup scheduled = new Pickup(new Laptop(2.0, Condition.WORKING, "Home", "x"), "Home");
        scheduled.setStatus(PickupStatus.SCHEDULED);
        when(pickupRepository.findById(1L)).thenReturn(Optional.of(scheduled));
        assertThat(service.updateStatus(1L, PickupStatus.COLLECTED).getStatus()).isEqualTo(PickupStatus.COLLECTED);

        Pickup collected = new Pickup(new Laptop(2.0, Condition.WORKING, "Home", "x"), "Home");
        collected.setStatus(PickupStatus.COLLECTED);
        when(pickupRepository.findById(2L)).thenReturn(Optional.of(collected));
        assertThat(service.updateStatus(2L, PickupStatus.RECYCLED).getStatus()).isEqualTo(PickupStatus.RECYCLED);
    }

    @Test
    void updateStatusRejectsSkippingStates() {
        PickupService service = newService();
        Pickup pending = new Pickup(new Laptop(2.0, Condition.WORKING, "Home", "x"), "Home");
        when(pickupRepository.findById(1L)).thenReturn(Optional.of(pending));

        // PENDING -> COLLECTED skips ASSIGNED and SCHEDULED.
        assertThatThrownBy(() -> service.updateStatus(1L, PickupStatus.COLLECTED))
                .isInstanceOf(InvalidPickupStatusTransitionException.class);
        // PENDING -> RECYCLED skips everything.
        assertThatThrownBy(() -> service.updateStatus(1L, PickupStatus.RECYCLED))
                .isInstanceOf(InvalidPickupStatusTransitionException.class);
    }

    @Test
    void updateStatusRejectsMovingBackwards() {
        PickupService service = newService();
        Pickup scheduled = new Pickup(new Laptop(2.0, Condition.WORKING, "Home", "x"), "Home");
        scheduled.setStatus(PickupStatus.SCHEDULED);
        when(pickupRepository.findById(1L)).thenReturn(Optional.of(scheduled));

        assertThatThrownBy(() -> service.updateStatus(1L, PickupStatus.ASSIGNED))
                .isInstanceOf(InvalidPickupStatusTransitionException.class);
    }

    @Test
    void updateStatusRejectsAnyChangeOnceRecycled() {
        PickupService service = newService();
        Pickup recycled = new Pickup(new Laptop(2.0, Condition.WORKING, "Home", "x"), "Home");
        recycled.setStatus(PickupStatus.RECYCLED);
        when(pickupRepository.findById(1L)).thenReturn(Optional.of(recycled));

        assertThatThrownBy(() -> service.updateStatus(1L, PickupStatus.PENDING))
                .isInstanceOf(InvalidPickupStatusTransitionException.class);
    }

    @Test
    void updateStatusThrowsWhenPickupDoesNotExist() {
        PickupService service = newService();
        when(pickupRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(404L, PickupStatus.COLLECTED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void pendingPickupsAreOrderedByHazardPriorityNotArrivalOrder() {
        PickupService service = newService();

        Pickup accessoryPickup = new Pickup(new Accessory(0.2, Condition.WORKING, "Home", "x"), "Home");
        Pickup laptopPickup = new Pickup(new Laptop(2.5, Condition.WORKING, "Home", "x"), "Home");
        Pickup batteryPickup = new Pickup(new Battery(1.0, Condition.DAMAGED, "Home", "x"), "Home");
        Pickup phonePickup = new Pickup(new MobilePhone(0.3, Condition.WORKING, "Home", "x"), "Home");

        // Deliberately supplied out of priority order: accessory, laptop, battery, phone.
        when(pickupRepository.findByStatus(PickupStatus.PENDING))
                .thenReturn(List.of(accessoryPickup, laptopPickup, batteryPickup, phonePickup));

        List<Pickup> ordered = service.getPendingPickupsByPriority();

        assertThat(ordered).extracting(p -> p.getItem().getItemType())
                .containsExactly("Battery", "Laptop", "Mobile Phone", "Accessory");
    }
}
