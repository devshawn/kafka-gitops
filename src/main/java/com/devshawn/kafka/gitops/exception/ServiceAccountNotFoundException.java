package com.devshawn.kafka.gitops.exception;

public class ServiceAccountNotFoundException extends RuntimeException {

    public ServiceAccountNotFoundException(String service) {
        super(String.format("Service account not found for service: %s", service));
    }
}
