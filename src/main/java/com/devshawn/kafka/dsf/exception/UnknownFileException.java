package com.devshawn.kafka.dsf.exception;

public class UnknownFileException extends RuntimeException {

    public UnknownFileException() {
        super("The requested desired state file could not be found or is empty.");
    }
}
