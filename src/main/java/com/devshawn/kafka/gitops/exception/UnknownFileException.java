package com.devshawn.kafka.gitops.exception;

public class UnknownFileException extends RuntimeException {

    public UnknownFileException() {
        super("The requested desired state file could not be found or is empty.");
    }
}
