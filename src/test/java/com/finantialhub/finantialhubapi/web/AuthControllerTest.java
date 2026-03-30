package com.finantialhub.finantialhubapi.web;

import tools.jackson.databind.ObjectMapper;
import com.finantialhub.finantialhubapi.application.usecases.AuthService;
import com.finantialhub.finantialhubapi.domain.exceptions.InvalidCredentialsException;
import com.finantialhub.finantialhubapi.domain.model.Role;
import com.finantialhub.finantialhubapi.domain.model.User;
import com.finantialhub.finantialhubapi.infrastructure.security.JwtService;
import com.finantialhub.finantialhubapi.infrastructure.web.AuthController;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.UserDtos.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_shouldReturnUserInfo_whenCredentialsAreValid() throws Exception {
        UUID id = UUID.randomUUID();

        User user = new User(
                id,
                "user@example.com",
                "John Doe",
                "hashed-password",
                Instant.now(),
                Role.MEMBER
        );

        when(authService.authenticate("user@example.com", "Abcdef12!"))
                .thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("mocked-jwt-token");

        LoginRequest request = new LoginRequest("user@example.com", "Abcdef12!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.token").value("mocked-jwt-token"));
    }

    @Test
    void login_shouldReturn401_whenCredentialsAreInvalid() throws Exception {
        when(authService.authenticate(anyString(), anyString()))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        LoginRequest request = new LoginRequest("wrong@example.com", "wrong");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                // adjust these jsonPath expectations if your ErrorResponse is different
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
