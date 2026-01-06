package com.finantialhub.finantialhubapi.infrastructure.web.dto;

import com.finantialhub.finantialhubapi.domain.model.Role;
import com.finantialhub.finantialhubapi.domain.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class UserDtos {

    public record CreateUserRequest(

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Full name is required")
            @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
            String fullName,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 128, message = "Password must be at least 8 characters long")
            String password
    ) {}

    public record UserResponse(
            UUID id,
            String email,
            String fullName,
            Role role,
            Instant createdAt
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole(),
                    user.getCreatedAt()
            );
        }
    }

    public record UpdateUserRequest(

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Full name is required")
            @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
            String fullName
    ) {}
}
