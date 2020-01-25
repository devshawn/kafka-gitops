package com.devshawn.kafka.dsf.exception;

public class MissingConfigurationException extends RuntimeException {

    public MissingConfigurationException(String environmentVariableName) {
        super(String.format("Missing required configuration: %s", environmentVariableName));
    }
}
