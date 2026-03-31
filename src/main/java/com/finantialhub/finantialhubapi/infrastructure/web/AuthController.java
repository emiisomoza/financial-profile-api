package com.finantialhub.finantialhubapi.infrastructure.web;

import com.finantialhub.finantialhubapi.application.usecases.AuthService;
import com.finantialhub.finantialhubapi.domain.model.User;
import com.finantialhub.finantialhubapi.infrastructure.security.JwtService;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.UserDtos.LoginRequest;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.UserDtos.LoginResponse;
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
