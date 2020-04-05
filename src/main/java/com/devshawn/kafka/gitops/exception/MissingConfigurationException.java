package com.devshawn.kafka.gitops.exception;

public class MissingConfigurationException extends RuntimeException {

    public MissingConfigurationException(String environmentVariableName) {
        super(String.format("Missing required configuration: %s", environmentVariableName));
    }
}
