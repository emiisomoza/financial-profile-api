package com.financialhub.financialhubapi.infrastructure.persistence;

import com.financialhub.financialhubapi.domain.model.Income;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.UUID;

public interface IncomeRepository extends ListCrudRepository<Income, UUID> {

    List<Income> findByUserId(UUID userId);
}
