package com.finantialhub.finantialhubapi.web;

import com.finantialhub.finantialhubapi.domain.exceptions.AssetNotFoundException;
import com.finantialhub.finantialhubapi.infrastructure.web.AssetController;
import com.finantialhub.finantialhubapi.infrastructure.web.dto.AssetDtos;
import tools.jackson.databind.ObjectMapper;
import com.finantialhub.finantialhubapi.application.usecases.AssetService;
import com.finantialhub.finantialhubapi.domain.model.Asset;
import com.finantialhub.finantialhubapi.domain.model.AssetType;
import com.finantialhub.finantialhubapi.domain.model.valuation.ValuationMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AssetController.class)
class AssetControllerTest {

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
    AssetService assetService;

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

    private Asset sampleAsset(UUID assetId, UUID userId) {
        return new Asset(assetId, userId, AssetType.CRYPTO, "Bitcoin", "BTCUSDT",
                new BigDecimal("0.5"), ValuationMode.MARKET, null, "USD", Instant.now());
    }

    // ── POST /api/v1/assets ──────────────────────────────────────────────────

    @Test
    void createAsset_memberExtractsUserIdFromJwt() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        when(assetService.createAsset(any())).thenReturn(sampleAsset(assetId, userId));

        mockMvc.perform(post("/api/v1/assets")
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"type":"CRYPTO","name":"Bitcoin","symbol":"BTCUSDT",
                             "quantity":0.5,"valuationMode":"MARKET","currency":"USD"}
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.type").value("CRYPTO"));
    }

    @Test
    void createAsset_memberIgnoresUserIdInBody() throws Exception {
        UUID jwtUserId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        when(assetService.createAsset(any())).thenReturn(sampleAsset(assetId, jwtUserId));

        mockMvc.perform(post("/api/v1/assets")
                        .with(memberJwt(jwtUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"userId":"%s","type":"CRYPTO","name":"Bitcoin","symbol":"BTCUSDT",
                             "quantity":0.5,"valuationMode":"MARKET","currency":"USD"}
                            """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(jwtUserId.toString()));
    }

    @Test
    void createAsset_adminCanSpecifyAnotherUserId() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        when(assetService.createAsset(any())).thenReturn(sampleAsset(assetId, targetUserId));

        mockMvc.perform(post("/api/v1/assets")
                        .with(adminJwt(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"userId":"%s","type":"CRYPTO","name":"Bitcoin","symbol":"BTCUSDT",
                             "quantity":0.5,"valuationMode":"MARKET","currency":"USD"}
                            """.formatted(targetUserId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(targetUserId.toString()));
    }

    // ── GET /api/v1/assets ───────────────────────────────────────────────────

    @Test
    void listAssets_memberGetsOwnAssetsWithoutParam() throws Exception {
        UUID userId = UUID.randomUUID();
        Asset asset = sampleAsset(UUID.randomUUID(), userId);
        when(assetService.getAssetsForUser(userId)).thenReturn(List.of(asset));

        mockMvc.perform(get("/api/v1/assets")
                        .with(memberJwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));
    }

    @Test
    void listAssets_adminCanQueryOtherUser() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        Asset asset = sampleAsset(UUID.randomUUID(), targetUserId);
        when(assetService.getAssetsForUser(targetUserId)).thenReturn(List.of(asset));

        mockMvc.perform(get("/api/v1/assets")
                        .param("userId", targetUserId.toString())
                        .with(adminJwt(adminId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(targetUserId.toString()));
    }

    // ── PUT /api/v1/assets/{id} ──────────────────────────────────────────────

    @Test
    void updateAsset_memberCanUpdateOwnAsset() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        Asset existing = sampleAsset(assetId, userId);
        Asset updated = new Asset(assetId, userId, AssetType.CRYPTO, "Bitcoin Updated", "BTC",
                new BigDecimal("1.0"), ValuationMode.MARKET, null, "AUD", Instant.now());

        when(assetService.getAssetById(assetId)).thenReturn(existing);
        when(assetService.updateAsset(eq(assetId), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/assets/" + assetId)
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssetDtos.UpdateAssetRequest("Bitcoin Updated", "BTC",
                                        new BigDecimal("1.0"), "MARKET", null, "AUD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bitcoin Updated"));
    }

    @Test
    void updateAsset_memberCannotUpdateOtherUsersAsset() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        Asset existingOtherUser = sampleAsset(assetId, otherUserId);

        when(assetService.getAssetById(assetId)).thenReturn(existingOtherUser);

        mockMvc.perform(put("/api/v1/assets/" + assetId)
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssetDtos.UpdateAssetRequest("Bitcoin", "BTC",
                                        new BigDecimal("1.0"), "MARKET", null, "AUD"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAsset_adminCanUpdateAnyAsset() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        Asset existing = sampleAsset(assetId, targetUserId);
        Asset updated = new Asset(assetId, targetUserId, AssetType.CRYPTO, "Bitcoin Updated", "BTC",
                new BigDecimal("1.0"), ValuationMode.MARKET, null, "AUD", Instant.now());

        when(assetService.getAssetById(assetId)).thenReturn(existing);
        when(assetService.updateAsset(eq(assetId), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/assets/" + assetId)
                        .with(adminJwt(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssetDtos.UpdateAssetRequest("Bitcoin Updated", "BTC",
                                        new BigDecimal("1.0"), "MARKET", null, "AUD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bitcoin Updated"));
    }

    @Test
    void updateAsset_returns404WhenAssetNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        when(assetService.getAssetById(assetId)).thenThrow(new AssetNotFoundException(assetId));

        mockMvc.perform(put("/api/v1/assets/" + assetId)
                        .with(memberJwt(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssetDtos.UpdateAssetRequest("Bitcoin", "BTC",
                                        new BigDecimal("1.0"), "MARKET", null, "AUD"))))
                .andExpect(status().isNotFound());
    }
}
