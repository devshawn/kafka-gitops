package com.devshawn.kafka.gitops.exception;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class MissingMultipleConfigurationException extends RuntimeException {

    public MissingMultipleConfigurationException(String... environmentVariableNames) {
        super(String.format("Missing required configuration(s): %s",
                Arrays.stream(environmentVariableNames).map(Objects::toString).collect(Collectors.joining(", "))));
    }
}
