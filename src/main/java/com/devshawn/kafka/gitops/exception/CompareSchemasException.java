package com.devshawn.kafka.gitops.exception;

public class CompareSchemasException extends RuntimeException {

    public CompareSchemasException(String exMessage) {
        super(String.format("Error comparing schemas: %s", exMessage));
    }
}
