package com.devshawn.kafka.gitops.exception;

import java.util.List;

public class InvalidAclDefinitionException extends RuntimeException {

    private final String field;

    private final String value;

    private final List<String> allowedValues;

    public InvalidAclDefinitionException(String field, String value, List<String> allowedValues) {
        this.field = field;
        this.value = value;
        this.allowedValues = allowedValues;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }

    public List<String> getAllowedValues() {
        return allowedValues;
    }
}
