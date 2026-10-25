package com.airporttransfer.admin;

import com.airporttransfer.booking.BookingRepository;
import com.airporttransfer.booking.BookingStatus;
import com.airporttransfer.common.ApiException;
import com.airporttransfer.common.PageDto;
import com.airporttransfer.user.Role;
import com.airporttransfer.user.User;
import com.airporttransfer.user.UserDtos.AdminUpdateUserRequest;
import com.airporttransfer.user.UserDtos.UserDto;
import com.airporttransfer.user.UserMapper;
import com.airporttransfer.user.UserRepository;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account management by admins. Safety rules:
 * an admin cannot demote or disable their own account (so at least one active admin always remains),
 * and a driver with an active trip (ASSIGNED or IN_PROGRESS) cannot be disabled or moved to another role.
 * Role and status changes apply to new logins; an already issued token stays valid until it expires.
 */
@Service
public class AdminUserService {

    static final int MAX_PAGE_SIZE = 100;
    private static final Set<BookingStatus> DRIVER_BUSY = Set.of(BookingStatus.ASSIGNED, BookingStatus.IN_PROGRESS);

    private final UserRepository users;
    private final BookingRepository bookings;
    private final UserMapper mapper;

    public AdminUserService(UserRepository users, BookingRepository bookings, UserMapper mapper) {
        this.users = users;
        this.bookings = bookings;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageDto<UserDto> list(String q, Role role, Boolean enabled, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Order.asc("fullName"), Sort.Order.asc("id")));
        return PageDto.of(users.findAll(UserSpecifications.matching(q, role, enabled), pageable),
            list -> list.stream().map(mapper::toDto).toList());
    }

    @Transactional(readOnly = true)
    public UserDto get(Long id) {
        return mapper.toDto(find(id));
    }

    @Transactional
    public UserDto update(Long actingAdminId, Long id, AdminUpdateUserRequest request) {
        User user = find(id);
        boolean enabled = request.enabled();
        if (user.getId().equals(actingAdminId) && (request.role() != Role.ADMIN || !enabled)) {
            throw ApiException.conflict("CANNOT_MODIFY_OWN_ACCESS", "You cannot demote or disable your own account");
        }
        boolean leavesDriverDuty = user.getRole() == Role.DRIVER && (request.role() != Role.DRIVER || !enabled);
        if (leavesDriverDuty && bookings.existsByAssignedDriverIdAndStatusIn(user.getId(), DRIVER_BUSY)) {
            throw ApiException.conflict("DRIVER_HAS_ACTIVE_TRIPS",
                "This driver still has assigned trips: reassign them first");
        }
        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();
        user.applyAdminUpdate(request.fullName().trim(), phone, request.role(), enabled);
        return mapper.toDto(users.save(user));
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
    }
}
