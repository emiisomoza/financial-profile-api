package com.financialhub.financialhubapi.domain.validation;

public interface UserRegistrationValidator {
    void validate(String email, String fullName, String rawPassword);
}
