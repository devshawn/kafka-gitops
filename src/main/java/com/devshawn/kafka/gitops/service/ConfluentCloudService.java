package com.devshawn.kafka.gitops.service;

import com.devshawn.kafka.gitops.domain.confluent.ServiceAccount;
import com.devshawn.kafka.gitops.exception.ConfluentCloudException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class ConfluentCloudService {

    private static org.slf4j.Logger log = LoggerFactory.getLogger(ConfluentCloudService.class);

    private final ObjectMapper objectMapper;
    private static final String ccloudExecutable;

    public ConfluentCloudService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<ServiceAccount> getServiceAccounts() {
        log.info("Fetching service account list from Confluent Cloud via ccloud tool.");
        try {
            String result = execCmd(new String[]{ccloudExecutable, "service-account", "list", "-o", "json"});
            return objectMapper.readValue(result, new TypeReference<List<ServiceAccount>>() {
            });
        } catch (IOException ex) {
            log.info(ex.getMessage());
            throw new ConfluentCloudException("There was an error listing Confluent Cloud service accounts. Are you logged in?");
        }
    }

    public ServiceAccount createServiceAccount(String name, boolean isUser) {
        log.info("Creating service account {} in Confluent Cloud via ccloud tool.", name);
        try {
            String serviceName = isUser ? String.format("user-%s", name) : name;
            String description = isUser ? String.format("User: %s", name) : String.format("Service account: %s", name);
            String result = execCmd(new String[]{ccloudExecutable, "service-account", "create", serviceName, "--description", description, "-o", "json"});
            return objectMapper.readValue(result, ServiceAccount.class);
        } catch (IOException ex) {
            throw new ConfluentCloudException(String.format("There was an error creating Confluent Cloud service account: %s.", name));
        }
    }

    public static String execCmd(String[] cmd) throws java.io.IOException {
        try (java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");) {
            return s.hasNext() ? s.next() : "";
        }
    }

    static {
        ccloudExecutable = System.getenv("CCLOUD_EXECUTABLE_PATH") != null ? System.getenv("CCLOUD_EXECUTABLE_PATH") : "ccloud";
        log.info("Using ccloud executable at: {}", ccloudExecutable);
    }
}
