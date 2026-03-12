package com.finantialhub.finantialhubapi.web;

import com.finantialhub.finantialhubapi.infrastructure.web.AssetController;
import tools.jackson.databind.ObjectMapper;
import com.finantialhub.finantialhubapi.application.usecases.AssetService;
import com.finantialhub.finantialhubapi.domain.model.Asset;
import com.finantialhub.finantialhubapi.domain.model.AssetType;
import com.finantialhub.finantialhubapi.domain.model.valuation.ValuationMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AssetController.class)
class AssetControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AssetService assetService;

    @Test
    void createAsset_returns201AndBody() throws Exception {
        UUID userId = UUID.randomUUID();
        CreateAssetRequest request = new CreateAssetRequest(
                userId.toString(),
                "CRYPTO",
                "Bitcoin",
                "BTCUSDT",
                new BigDecimal("0.5"),
                "MARKET",
                null,
                "USD"
        );

        Asset asset = new Asset(
                UUID.randomUUID(),
                userId,
                AssetType.CRYPTO,
                "Bitcoin",
                "BTCUSDT",
                new BigDecimal("0.5"),
                ValuationMode.MARKET,
                null,
                "USD",
                Instant.now()
        );

        when(assetService.createAsset(any())).thenReturn(asset);

        mockMvc.perform(post("/api/v1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(asset.getId().toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.type").value("CRYPTO"))
                .andExpect(jsonPath("$.name").value("Bitcoin"))
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.quantity").value(0.5))
                .andExpect(jsonPath("$.valuationMode").value("MARKET"))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void listAssets_returns200AndList() throws Exception {
        UUID userId = UUID.randomUUID();

        Asset asset1 = new Asset(
                UUID.randomUUID(),
                userId,
                AssetType.CRYPTO,
                "Bitcoin",
                "BTCUSDT",
                new BigDecimal("0.5"),
                ValuationMode.MARKET,
                null,
                "USD",
                Instant.now()
        );

        when(assetService.getAssetsForUser(userId))
                .thenReturn(List.of(asset1));

        mockMvc.perform(get("/api/v1/assets")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(asset1.getId().toString()))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].type").value("CRYPTO"));
    }

    // simple DTO for test
    record CreateAssetRequest(
            String userId,
            String type,
            String name,
            String symbol,
            BigDecimal quantity,
            String valuationMode,
            BigDecimal manualUnitValue,
            String currency
    ) {}
}
