package com.finantialhub.finantialhubapi.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class UserDtos {

    public record CreateUserRequest(
            @Email @NotBlank String email,
            @NotBlank String fullName,
            @NotBlank String password
    ) {}

    public record UserResponse(
            UUID id,
            String email,
            String fullName
    ) {}
}
