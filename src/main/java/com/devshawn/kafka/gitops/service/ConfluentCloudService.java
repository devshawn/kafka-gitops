package com.devshawn.kafka.gitops.service;

import com.devshawn.kafka.gitops.config.KafkaGitopsConfigLoader;
import com.devshawn.kafka.gitops.domain.confluent.ServiceAccount;
import com.devshawn.kafka.gitops.exception.ConfluentCloudException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class ConfluentCloudService {

    private static org.slf4j.Logger log = LoggerFactory.getLogger(KafkaGitopsConfigLoader.class);

    private final ObjectMapper objectMapper;

    public ConfluentCloudService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<ServiceAccount> getServiceAccounts() {
        log.info("Test");
        try {
            String result = execCmd(new String[]{"ccloud", "service-account", "list", "-o", "json"});
            return objectMapper.readValue(result, new TypeReference<List<ServiceAccount>>() {
            });
        } catch (IOException ex) {
            throw new ConfluentCloudException("There was an error listing Confluent Cloud service accounts. Are you logged in?");
        }
    }

    public ServiceAccount createServiceAccount(String name) {
        try {
            String description = String.format("Service account: %s", name);
            String result = execCmd(new String[]{"ccloud", "service-account", "create", name, "--description", description, "-o", "json"});
            return objectMapper.readValue(result, ServiceAccount.class);
        } catch (IOException ex) {
            throw new ConfluentCloudException(String.format("There was an error creating Confluent Cloud service account: %s.", name));
        }
    }

    public static String execCmd(String[] cmd) throws java.io.IOException {
        java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
