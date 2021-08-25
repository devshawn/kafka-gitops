package com.devshawn.kafka.gitops.enums;

public enum SchemaCompatibility {
    NONE,
    BACKWARD,
    FORWARD,
    FULL,
    BACKWARD_TRANSITIVE,
    FORWARD_TRANSITIVE,
    FULL_TRANSITIVE;
}
