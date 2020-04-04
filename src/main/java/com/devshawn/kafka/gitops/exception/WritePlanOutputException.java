package com.devshawn.kafka.gitops.exception;

public class WritePlanOutputException extends RuntimeException {

    public WritePlanOutputException(String exMessage) {
        super(String.format("Error writing execution plan to file: %s", exMessage));
    }
}
