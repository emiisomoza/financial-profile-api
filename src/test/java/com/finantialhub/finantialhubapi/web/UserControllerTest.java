package com.finantialhub.finantialhubapi.web;

import com.finantialhub.finantialhubapi.application.usecases.UserService;
import com.finantialhub.finantialhubapi.domain.exceptions.EmailAlreadyExistsException;
import com.finantialhub.finantialhubapi.domain.exceptions.InvalidFullNameException;
import com.finantialhub.finantialhubapi.domain.exceptions.WeakPasswordException;
import com.finantialhub.finantialhubapi.domain.model.User;
import com.finantialhub.finantialhubapi.infrastructure.web.UserController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    // This is provided to the controller as a mock, so we don't need DB or full context
    @MockitoBean
    UserService userService;

    @Test
    void createUser_returns201AndBody() throws Exception {
        var payload = new CreateUserRequest("alice@example.com", "Alice Doe", "secret123");

        // Arrange: mock service behavior
        var user = new User(
                UUID.randomUUID(),
                "alice@example.com",
                "Alice Doe",
                "hashed-secret",          // or raw for now
                Instant.now()
        );

        Mockito.when(userService.registerUser(anyString(), anyString(), anyString()))
                .thenReturn(user);

        // Act + Assert
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.fullName").value("Alice Doe"));
    }

    @Test
    void createUser_whenEmailAlreadyExists_returns409() throws Exception {
        var payload = new CreateUserRequest("alice@example.com", "Alice Doe", "secret123");

        // Arrange: service should throw our custom exception
        Mockito.when(userService.registerUser(anyString(), anyString(), anyString()))
                .thenThrow(new EmailAlreadyExistsException("Email already exists"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void createUser_whenInvalidFullNameException_returns400() throws Exception {
        var payload = new CreateUserRequest("alice@example.com", "Alice3", "secret123");

        // Arrange: service should throw our custom exception
        Mockito.when(userService.registerUser(anyString(), anyString(), anyString()))
                .thenThrow(new InvalidFullNameException("Full name cannot contain numbers"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FULL_NAME"))
                .andExpect(jsonPath("$.message").value("Full name cannot contain numbers"));
    }

    @Test
    void createUser_whenWeakPasswordException_returns400() throws Exception {
        var payload = new CreateUserRequest("alice@example.com", "Alice", "secret123");

        // Arrange: service should throw our custom exception
        Mockito.when(userService.registerUser(anyString(), anyString(), anyString()))
                .thenThrow(new WeakPasswordException("Password must be at least 8 characters long and include " +
                        "at least 2 digits, 1 uppercase letter and 1 special character"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"))
                .andExpect(jsonPath("$.message").value("Password must be at least 8 characters long and include " +
                        "at least 2 digits, 1 uppercase letter and 1 special character"));
    }

    @Test
    void whenInvalidRequest_thenReturns400AndFieldErrors() throws Exception {
        // email invalid, fullName blank, password too short
        var body = """
                {
                  "email": "not-an-email",
                  "fullName": "A",
                  "password": "123"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    // test-only record to build the JSON request body
    record CreateUserRequest(String email, String fullName, String password) {}
}
