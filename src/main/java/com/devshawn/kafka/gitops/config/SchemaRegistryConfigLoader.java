package com.devshawn.kafka.gitops.config;

import com.devshawn.kafka.gitops.exception.MissingConfigurationException;
import com.devshawn.kafka.gitops.exception.MissingMultipleConfigurationException;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SchemaRegistryConfigLoader {

    private static org.slf4j.Logger log = LoggerFactory.getLogger(SchemaRegistryConfigLoader.class);

    public static final String SCHEMA_REGISTRY_URL_KEY = "SCHEMA_REGISTRY_URL";
    public static final String SCHEMA_DIRECTORY_KEY = "SCHEMA_DIRECTORY";
    public static final String SCHEMA_REGISTRY_SASL_JAAS_USERNAME_KEY = "SCHEMA_REGISTRY_SASL_JAAS_USERNAME";
    public static final String SCHEMA_REGISTRY_SASL_JAAS_PASSWORD_KEY = "SCHEMA_REGISTRY_SASL_JAAS_PASSWORD";
    public static final String SCHEMA_REGISTRY_SASL_CONFIG_KEY = "SCHEMA_REGISTRY_SASL_CONFIG";

    private SchemaRegistryConfigLoader() {}

    public static SchemaRegistryConfig load() {
        SchemaRegistryConfig.Builder builder = new SchemaRegistryConfig.Builder();
        setConfig(builder);
        return builder.build();
    }

    private static void setConfig(SchemaRegistryConfig.Builder builder) {
        Map<String, Object> config = new HashMap<>();
        AtomicReference<String> username = new AtomicReference<>();
        AtomicReference<String> password = new AtomicReference<>();

        Map<String, String> environment = System.getenv();

        environment.forEach((key, value) -> {
            if (key.equals(SCHEMA_REGISTRY_SASL_JAAS_USERNAME_KEY)) {
                username.set(value);
            } else if (key.equals(SCHEMA_REGISTRY_SASL_JAAS_PASSWORD_KEY)) {
                password.set(value);
            } else if (key.equals(SCHEMA_REGISTRY_URL_KEY)) {
                config.put(SCHEMA_REGISTRY_URL_KEY, value);
            } else if (key.equals(SCHEMA_DIRECTORY_KEY)) {
                config.put(SCHEMA_DIRECTORY_KEY, value);
            }
        });

        handleDefaultConfig(config);
        handleAuthentication(username, password, config);

        log.info("Schema Registry Config: {}", config);

        builder.putAllConfig(config);
    }

    private static void handleDefaultConfig(Map<String, Object> config) {
        final String DEFAULT_URL = "http://localhost:8081";
        final String CURRENT_WORKING_DIR = System.getProperty("user.dir");
        if (!config.containsKey(SCHEMA_REGISTRY_URL_KEY)) {
            log.info("{} not set.  Using default value of {}", SCHEMA_REGISTRY_URL_KEY, DEFAULT_URL);
            config.put(SCHEMA_REGISTRY_URL_KEY, DEFAULT_URL);
        }
        if (!config.containsKey(SCHEMA_DIRECTORY_KEY)) {
            log.info("{} not set.  Defaulting to current working directory: {}", SCHEMA_DIRECTORY_KEY, CURRENT_WORKING_DIR);
            config.put(SCHEMA_DIRECTORY_KEY, CURRENT_WORKING_DIR);
        }
    }

    private static void handleAuthentication(AtomicReference<String> username, AtomicReference<String> password, Map<String, Object> config) {
        if (username.get() != null && password.get() != null) {
            String loginModule = "org.apache.kafka.common.security.plain.PlainLoginModule";
            String value = String.format("%s required username=\"%s\" password=\"%s\";",
                    loginModule, escape(username.get()), escape(password.get()));
            config.put(SCHEMA_REGISTRY_SASL_CONFIG_KEY, value);
        } else {
          if(config.get(SCHEMA_REGISTRY_SASL_CONFIG_KEY) == null) {
              log.info("{} or {} not set. No authentication configured for the Schema Registry",
                  SCHEMA_REGISTRY_SASL_JAAS_USERNAME_KEY, SCHEMA_REGISTRY_SASL_JAAS_PASSWORD_KEY);
          }
        }
    }

    private static String escape(String value) {
        if (value != null) {
            return value.replace("\"", "\\\"");
        }
        return null;
    }
}
