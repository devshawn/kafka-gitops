package com.devshawn.kafka.gitops.exception;

public class SchemaRegistryExecutionException extends RuntimeException {

    private final String exceptionMessage;

    public SchemaRegistryExecutionException(String message, String exceptionMessage) {
        super(message);
        this.exceptionMessage = exceptionMessage;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }
}
