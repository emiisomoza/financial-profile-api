package com.finantialhub.finantialhubapi.infrastructure.pricing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class PriceApiClient {

    private final RestClient restClient;

    public PriceApiClient(@Value("${price.api.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public BigDecimal fetchPrice(String assetType, String asset, String currency) {
        Map<?, ?> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/price")
                        .queryParam("assetType", assetType)
                        .queryParam("asset", asset)
                        .queryParam("currency", currency)
                        .build())
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("price")) {
            throw new RuntimeException("Invalid response from Price API for %s/%s".formatted(asset, currency));
        }

        return new BigDecimal(response.get("price").toString());
    }
}