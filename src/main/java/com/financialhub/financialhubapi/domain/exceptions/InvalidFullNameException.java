package com.financialhub.financialhubapi.domain.exceptions;

public class InvalidFullNameException extends RuntimeException {
    public InvalidFullNameException(String message) {
        super(message);
    }
}