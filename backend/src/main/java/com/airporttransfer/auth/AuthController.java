package com.airporttransfer.auth;

import com.airporttransfer.auth.AuthDtos.AuthResponse;
import com.airporttransfer.auth.AuthDtos.LoginRequest;
import com.airporttransfer.auth.JwtService.IssuedToken;
import com.airporttransfer.user.PasswordResetService;
import com.airporttransfer.user.User;
import com.airporttransfer.user.UserDtos.ChangePasswordRequest;
import com.airporttransfer.user.UserDtos.ForgotPasswordRequest;
import com.airporttransfer.user.UserDtos.ProfileRequest;
import com.airporttransfer.user.UserDtos.ResetPasswordRequest;
import com.airporttransfer.user.UserDtos.RegisterRequest;
import com.airporttransfer.user.UserDtos.UserDto;
import com.airporttransfer.user.UserMapper;
import com.airporttransfer.user.UserService;
import jakarta.validation.Valid;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService users;
    private final JwtService jwt;
    private final UserMapper mapper;
    private final PasswordResetService passwordReset;

    public AuthController(UserService users, JwtService jwt, UserMapper mapper, PasswordResetService passwordReset) {
        this.passwordReset = passwordReset;
        this.users = users;
        this.jwt = jwt;
        this.mapper = mapper;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = users.register(request.email(), request.password(), request.fullName(), request.phone());
        return respond(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return respond(users.authenticate(request.email(), request.password()));
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Jwt principal) {
        return mapper.toDto(users.getActive(Long.valueOf(principal.getSubject())));
    }

    @PutMapping("/me")
    public UserDto updateMe(@AuthenticationPrincipal Jwt principal, @Valid @RequestBody ProfileRequest request) {
        return mapper.toDto(users.updateProfile(Long.valueOf(principal.getSubject()), request.fullName(),
            request.phone()));
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt principal, @Valid @RequestBody ChangePasswordRequest request) {
        users.changePassword(Long.valueOf(principal.getSubject()), request.currentPassword(), request.newPassword());
    }

    /** Always 204, whether or not the e-mail has an account. */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordReset.request(request.email(), LocaleContextHolder.getLocale());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordReset.reset(request.token(), request.newPassword());
    }

    private AuthResponse respond(User user) {
        IssuedToken issued = jwt.issue(user);
        return new AuthResponse(issued.token(), "Bearer", issued.expiresAt(), mapper.toDto(user));
    }
}
