package com.devshawn.kafka.gitops.exception;

public class PlanIsUpToDateException extends RuntimeException {

    public PlanIsUpToDateException() {
        super("The current desired state file matches the actual state of the cluster.");
    }
}
