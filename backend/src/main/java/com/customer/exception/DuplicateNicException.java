package com.customer.exception;

public class DuplicateNicException extends RuntimeException {

    public DuplicateNicException(String nicNumber) {
        super("A customer with NIC number '" + nicNumber + "' already exists.");
    }
}
