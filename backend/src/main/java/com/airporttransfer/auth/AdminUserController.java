package com.airporttransfer.auth;

import com.airporttransfer.admin.AdminUserService;
import com.airporttransfer.common.PageDto;
import com.airporttransfer.user.Role;
import com.airporttransfer.user.User;
import com.airporttransfer.user.UserDtos.AdminCreateUserRequest;
import com.airporttransfer.user.UserDtos.AdminUpdateUserRequest;
import com.airporttransfer.user.UserDtos.UserDto;
import com.airporttransfer.user.UserMapper;
import com.airporttransfer.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Account management for admins (ROLE_ADMIN is enforced in SecurityConfig). Public sign-up only creates customers. */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService users;
    private final AdminUserService adminUsers;
    private final UserMapper mapper;

    public AdminUserController(UserService users, AdminUserService adminUsers, UserMapper mapper) {
        this.users = users;
        this.adminUsers = adminUsers;
        this.mapper = mapper;
    }

    @GetMapping
    public PageDto<UserDto> list(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) Role role,
                                 @RequestParam(required = false) Boolean enabled,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return adminUsers.list(q, role, enabled, page, size);
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable Long id) {
        return adminUsers.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody AdminCreateUserRequest request) {
        User user = users.create(request.email(), request.password(), request.fullName(), request.phone(),
            request.role());
        return mapper.toDto(user);
    }

    @PutMapping("/{id}")
    public UserDto update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                          @Valid @RequestBody AdminUpdateUserRequest request) {
        return adminUsers.update(Long.valueOf(jwt.getSubject()), id, request);
    }
}
