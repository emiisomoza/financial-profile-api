package com.financialhub.financialhubapi.infrastructure.persistence;

import com.financialhub.financialhubapi.domain.model.Expense;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends ListCrudRepository<Expense, UUID> {

    List<Expense> findByUserId(UUID userId);
}
