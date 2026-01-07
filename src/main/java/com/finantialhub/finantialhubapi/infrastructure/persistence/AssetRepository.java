package com.finantialhub.finantialhubapi.infrastructure.persistence;

import com.finantialhub.finantialhubapi.domain.model.Asset;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.UUID;

public interface AssetRepository extends ListCrudRepository<Asset, UUID> {

    List<Asset> findByUserId(UUID userId);
}
