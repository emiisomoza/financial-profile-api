package com.financialhub.financialhubapi.infrastructure.web;

import com.financialhub.financialhubapi.application.usecases.AuthService;
import com.financialhub.financialhubapi.domain.model.User;
import com.financialhub.financialhubapi.infrastructure.security.JwtService;
import com.financialhub.financialhubapi.infrastructure.web.dto.UserDtos.LoginRequest;
import com.financialhub.financialhubapi.infrastructure.web.dto.UserDtos.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.authenticate(request.email(), request.password());
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(LoginResponse.from(user, token));
    }
}
