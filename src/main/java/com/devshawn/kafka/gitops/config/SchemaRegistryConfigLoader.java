package com.devshawn.kafka.gitops.config;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;

public class SchemaRegistryConfigLoader {

    private static org.slf4j.Logger log = LoggerFactory.getLogger(SchemaRegistryConfigLoader.class);

    public static final String SCHEMA_REGISTRY_URL_KEY = "SCHEMA_REGISTRY_URL";
    public static final String SCHEMA_DIRECTORY_KEY = "SCHEMA_DIRECTORY";

    private SchemaRegistryConfigLoader() {}

    public static SchemaRegistryConfig load() {
        SchemaRegistryConfig.Builder builder = new SchemaRegistryConfig.Builder();
        setConfig(builder);
        return builder.build();
    }

    private static void setConfig(SchemaRegistryConfig.Builder builder) {
        Map<String, Object> config = new HashMap<>();

        Map<String, String> environment = System.getenv();

        environment.forEach((key, value) -> {
            if (key.equals(SCHEMA_REGISTRY_URL_KEY)) {
                config.put(SCHEMA_REGISTRY_URL_KEY, value);
            } else if (key.equals(SCHEMA_DIRECTORY_KEY)) {
                config.put(SCHEMA_DIRECTORY_KEY, value);
            } else if (key.startsWith("SCHEMA_REGISTRY_")) {
                String newKey = key.substring("SCHEMA_REGISTRY_".length()).replace("_", ".").toLowerCase();
                config.put(newKey, value);
          }
        });

        handleDefaultConfig(config);

        log.info("Schema Registry Config: {}", config);

        builder.putAllConfig(config);
    }

    private static void handleDefaultConfig(Map<String, Object> config) {
        final String CURRENT_WORKING_DIR = System.getProperty("user.dir");
        if (!config.containsKey(SCHEMA_DIRECTORY_KEY)) {
            log.info("{} not set. Defaulting to current working directory: {}", SCHEMA_DIRECTORY_KEY, CURRENT_WORKING_DIR);
            config.put(SCHEMA_DIRECTORY_KEY, CURRENT_WORKING_DIR);
        }
    }
}
