package com.reloop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.reloop.controller.EwasteItemRequest;
import com.reloop.exception.InvalidItemException;
import com.reloop.exception.ResourceNotFoundException;
import com.reloop.model.EwasteItem;
import com.reloop.model.Laptop;
import com.reloop.model.User;
import com.reloop.model.UserRole;
import com.reloop.repository.EwasteItemRepository;
import com.reloop.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EwasteServiceTest {

    @Mock
    private EwasteItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    private EwasteItemRequest validRequest() {
        EwasteItemRequest request = new EwasteItemRequest();
        request.setItemType("LAPTOP");
        request.setWeightKg(2.0);
        request.setCondition("WORKING");
        request.setLocation("College Hostel");
        request.setDescription("Test laptop");
        return request;
    }

    @Test
    void registerItemSavesTheCorrectSubclassForAValidRequest() {
        EwasteService service = new EwasteService(itemRepository, userRepository);
        when(itemRepository.save(any(EwasteItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EwasteItem saved = service.registerItem(validRequest());

        assertThat(saved).isInstanceOf(Laptop.class);
        assertThat(saved.getItemType()).isEqualTo("Laptop");
    }

    @Test
    void registerItemLinksTheOwnerWhenUserIdIsProvided() {
        EwasteService service = new EwasteService(itemRepository, userRepository);
        User citizen = new User("Demo Citizen", "citizen@example.com", UserRole.CITIZEN);
        when(userRepository.findById(7L)).thenReturn(Optional.of(citizen));
        when(itemRepository.save(any(EwasteItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EwasteItemRequest request = validRequest();
        request.setUserId(7L);

        EwasteItem saved = service.registerItem(request);

        assertThat(saved.getOwner()).isSameAs(citizen);
    }

    @Test
    void registerItemThrowsWhenUserIdDoesNotExist() {
        EwasteService service = new EwasteService(itemRepository, userRepository);
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        EwasteItemRequest request = validRequest();
        request.setUserId(404L);

        assertThatThrownBy(() -> service.registerItem(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registerItemRejectsUnknownItemType() {
        EwasteService service = new EwasteService(itemRepository, userRepository);
        EwasteItemRequest request = validRequest();
        request.setItemType("TOASTER");

        assertThatThrownBy(() -> service.registerItem(request))
                .isInstanceOf(InvalidItemException.class);
    }

    @Test
    void registerItemRejectsUnknownCondition() {
        EwasteService service = new EwasteService(itemRepository, userRepository);
        EwasteItemRequest request = validRequest();
        request.setCondition("MINT");

        assertThatThrownBy(() -> service.registerItem(request))
                .isInstanceOf(InvalidItemException.class);
    }

    @Test
    void registerItemRejectsBlankLocation() {
        EwasteService service = new EwasteService(itemRepository, userRepository);
        EwasteItemRequest request = validRequest();
        request.setLocation("   ");

        assertThatThrownBy(() -> service.registerItem(request))
                .isInstanceOf(InvalidItemException.class);
    }

    @Test
    void registerItemRejectsNonPositiveWeight() {
        EwasteService service = new EwasteService(itemRepository, userRepository);
        EwasteItemRequest request = validRequest();
        request.setWeightKg(0);

        assertThatThrownBy(() -> service.registerItem(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getItemByIdThrowsWhenMissing() {
        EwasteService service = new EwasteService(itemRepository, userRepository);
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getItemById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
