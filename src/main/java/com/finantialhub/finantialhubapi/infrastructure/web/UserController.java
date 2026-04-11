package com.finantialhub.finantialhubapi.infrastructure.web;

import com.finantialhub.finantialhubapi.application.usecases.UserService;
import com.finantialhub.finantialhubapi.domain.model.User;
import com.finantialhub.finantialhubapi.infrastructure.security.SecurityUtils;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.UserDtos.UpdateUserRequest;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.UserDtos.CreateUserRequest;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.UserDtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.registerUser(request.email(), request.fullName(), request.password());
        UserResponse body = new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getCreatedAt());
        URI location = URI.create("/api/v1/users/" + user.getId());
        return ResponseEntity.created(location).body(body);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> response = userService.getAllUsers().stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public UserResponse getUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        SecurityUtils.requireOwnership(jwt, id);

        User user = userService.getUser(id);
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getCreatedAt());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {

        SecurityUtils.requireOwnership(jwt, id);

        User updated = userService.updateUser(id, request.email(), request.fullName());
        return ResponseEntity.ok(UserResponse.from(updated));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/{id}/promote")
    public ResponseEntity<UserResponse> promoteUserToAdmin(@PathVariable UUID id) {
        User promoted = userService.promoteUserToAdmin(id);
        return ResponseEntity.ok(UserResponse.from(promoted));
    }
}
