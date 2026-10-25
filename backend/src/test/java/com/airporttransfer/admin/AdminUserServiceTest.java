package com.airporttransfer.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.airporttransfer.booking.BookingRepository;
import com.airporttransfer.common.ApiException;
import com.airporttransfer.user.Role;
import com.airporttransfer.user.User;
import com.airporttransfer.user.UserDtos.AdminUpdateUserRequest;
import com.airporttransfer.user.UserMapper;
import com.airporttransfer.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AdminUserServiceTest {

    UserRepository users = mock(UserRepository.class);
    BookingRepository bookings = mock(BookingRepository.class);
    UserMapper mapper = mock(UserMapper.class);
    AdminUserService service = new AdminUserService(users, bookings, mapper);

    @BeforeEach
    void save() {
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    private User user(long id, Role role) {
        User u = new User("u" + id + "@example.tn", "hash", "User " + id, null, role);
        ReflectionTestUtils.setField(u, "id", id);
        when(users.findById(id)).thenReturn(Optional.of(u));
        return u;
    }

    private static AdminUpdateUserRequest request(Role role, boolean enabled) {
        return new AdminUpdateUserRequest("New Name", " ", role, enabled);
    }

    @Test
    void adminCannotDemoteOrDisableSelf() {
        user(1, Role.ADMIN);
        assertThatThrownBy(() -> service.update(1L, 1L, request(Role.MODERATOR, true)))
            .isInstanceOf(ApiException.class).extracting("code").isEqualTo("CANNOT_MODIFY_OWN_ACCESS");
        assertThatThrownBy(() -> service.update(1L, 1L, request(Role.ADMIN, false)))
            .isInstanceOf(ApiException.class).extracting("code").isEqualTo("CANNOT_MODIFY_OWN_ACCESS");
        verify(users, never()).save(any());
    }

    @Test
    void adminCanEditOwnNameAndKeepAccess() {
        User admin = user(1, Role.ADMIN);
        service.update(1L, 1L, request(Role.ADMIN, true));
        assertThat(admin.getFullName()).isEqualTo("New Name");
        assertThat(admin.getPhone()).isNull();
    }

    @Test
    void busyDriverCannotBeDisabledOrMoved() {
        user(2, Role.DRIVER);
        when(bookings.existsByAssignedDriverIdAndStatusIn(any(), anyCollection())).thenReturn(true);
        assertThatThrownBy(() -> service.update(1L, 2L, request(Role.DRIVER, false)))
            .isInstanceOf(ApiException.class).extracting("code").isEqualTo("DRIVER_HAS_ACTIVE_TRIPS");
        assertThatThrownBy(() -> service.update(1L, 2L, request(Role.CUSTOMER, true)))
            .isInstanceOf(ApiException.class).extracting("code").isEqualTo("DRIVER_HAS_ACTIVE_TRIPS");
    }

    @Test
    void freeDriverCanBeDisabledAndStaysDriverWhileBusyWithNoChange() {
        User driver = user(2, Role.DRIVER);
        when(bookings.existsByAssignedDriverIdAndStatusIn(any(), anyCollection())).thenReturn(false);
        service.update(1L, 2L, request(Role.DRIVER, false));
        assertThat(driver.isEnabled()).isFalse();

        // Editing only the name of a busy driver is fine.
        User busy = user(3, Role.DRIVER);
        when(bookings.existsByAssignedDriverIdAndStatusIn(any(), anyCollection())).thenReturn(true);
        service.update(1L, 3L, request(Role.DRIVER, true));
        assertThat(busy.getFullName()).isEqualTo("New Name");
    }

    @Test
    void unknownUserIs404() {
        when(users.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(1L, 99L, request(Role.CUSTOMER, true)))
            .isInstanceOf(ApiException.class).extracting("code").isEqualTo("USER_NOT_FOUND");
    }
}
