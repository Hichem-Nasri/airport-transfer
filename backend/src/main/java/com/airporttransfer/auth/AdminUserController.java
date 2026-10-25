package com.airporttransfer.auth;

import com.airporttransfer.user.User;
import com.airporttransfer.user.UserDtos.AdminCreateUserRequest;
import com.airporttransfer.user.UserDtos.UserDto;
import com.airporttransfer.user.UserMapper;
import com.airporttransfer.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** DRIVER and ADMIN accounts are created here, never by public sign-up. ROLE_ADMIN is enforced in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService users;
    private final UserMapper mapper;

    public AdminUserController(UserService users, UserMapper mapper) {
        this.users = users;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody AdminCreateUserRequest request) {
        User user = users.create(request.email(), request.password(), request.fullName(), request.phone(),
            request.role());
        return mapper.toDto(user);
    }
}
