package com.finantialhub.finantialhubapi.web;

import com.finantialhub.finantialhubapi.application.usecases.UserService;
import com.finantialhub.finantialhubapi.domain.exceptions.EmailAlreadyExistsException;
import com.finantialhub.finantialhubapi.domain.exceptions.InvalidFullNameException;
import com.finantialhub.finantialhubapi.domain.exceptions.WeakPasswordException;
import com.finantialhub.finantialhubapi.domain.model.Role;
import com.finantialhub.finantialhubapi.domain.model.User;
import com.finantialhub.finantialhubapi.infrastructure.web.UserController;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.UserDtos.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
class UserControllerTest {

    @TestConfiguration
    static class SecurityTestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static RequestPostProcessor memberJwt(UUID userId) {
        return jwtContext(userId, "MEMBER");
    }

    private static RequestPostProcessor adminJwt(UUID adminId) {
        return jwtContext(adminId, "ADMIN");
    }

    private static RequestPostProcessor jwtContext(UUID userId, String role) {
        return request -> {
            Jwt jwt = Jwt.withTokenValue("test-token")
                    .header("alg", "HS256")
                    .subject(userId.toString())
                    .claim("role", role)
                    .build();
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt,
                    List.of(new SimpleGrantedAuthority(role)));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            return request;
        };
    }

    @Test
    void createUser_returns201AndBody() throws Exception {
        var payload = new CreateUserRequest("alice@example.com", "Alice Doe", "secret123");

        var user = new User(
                UUID.randomUUID(),
                "alice@example.com",
                "Alice Doe",
                "hashed-secret",
                Instant.now(),
                Role.MEMBER
        );

        when(userService.registerUser(anyString(), anyString(), anyString()))
                .thenReturn(user);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.fullName").value("Alice Doe"))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void createUser_whenEmailAlreadyExists_returns409() throws Exception {
        var payload = new CreateUserRequest("alice@example.com", "Alice Doe", "secret123");

        when(userService.registerUser(anyString(), anyString(), anyString()))
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

        when(userService.registerUser(anyString(), anyString(), anyString()))
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

        when(userService.registerUser(anyString(), anyString(), anyString()))
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

    @Test
    void getAllUsers_adminCanListAllUsers() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        User user = new User(id, "test@example.com", "John Doe", "hash", Instant.now(), Role.MEMBER);

        when(userService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/v1/users")
                        .with(adminJwt(adminId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].email").value("test@example.com"))
                .andExpect(jsonPath("$[0].fullName").value("John Doe"));
    }

    @Test
    void getUserById_memberGetsOwnProfile() throws Exception {
        UUID id = UUID.randomUUID();
        User user = new User(id, "test@example.com", "John Doe", "hash", Instant.now(), Role.MEMBER);

        when(userService.getUser(id)).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/" + id)
                        .with(memberJwt(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void getUserById_memberCannotAccessOtherUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/users/" + otherId)
                        .with(memberJwt(userId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_memberCanUpdateOwnProfile() throws Exception {
        UUID id = UUID.randomUUID();

        User user = new User(
                id,
                "new@example.com",
                "New Name",
                "hash",
                Instant.now(),
                Role.MEMBER
        );

        when(userService.updateUser(eq(id), eq("new@example.com"), eq("New Name")))
                .thenReturn(user);

        UpdateUserRequest request = new UpdateUserRequest("new@example.com", "New Name");

        mockMvc.perform(put("/api/v1/users/" + id)
                        .with(memberJwt(id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.fullName").value("New Name"))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void updateUser_memberCannotUpdateOtherUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        UpdateUserRequest request = new UpdateUserRequest("new@example.com", "New Name");

        mockMvc.perform(put("/api/v1/users/" + otherId)
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // test-only record to build the JSON request body
    record CreateUserRequest(String email, String fullName, String password) {}
}
