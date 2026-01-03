package com.finantialhub.finantialhubapi.domain.exceptions;

public class InvalidFullNameException extends RuntimeException {
    public InvalidFullNameException(String message) {
        super(message);
    }
}