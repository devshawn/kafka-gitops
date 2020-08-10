package com.devshawn.kafka.gitops.exception;

public class ReadPlanInputException extends RuntimeException {

    public ReadPlanInputException() {
        super("Error reading execution plan from file: Please run the plan command again to generate a new plan file.");
    }

    public ReadPlanInputException(String message) {
        super(String.format("Error reading execution plan from file: %s", message));
    }
}
