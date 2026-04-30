package com.financialhub.financialhubapi.infrastructure.persistence;

import com.financialhub.financialhubapi.domain.model.User;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends ListCrudRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
