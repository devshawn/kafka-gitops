package com.devshawn.kafka.gitops.config;

import com.devshawn.kafka.gitops.exception.MissingConfigurationException;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class KafkaGitopsConfigLoader {

    private static org.slf4j.Logger log = LoggerFactory.getLogger(KafkaGitopsConfigLoader.class);

    public static KafkaGitopsConfig load() {
        return load(null);
    }

    public static KafkaGitopsConfig load(File configFile) {
        KafkaGitopsConfig.Builder builder = new KafkaGitopsConfig.Builder();
        setConfigFromFile(configFile, builder);
        setConfigFromEnvironment(builder);
        return builder.build();
    }

    private static void setConfigFromFile(File configFile, KafkaGitopsConfig.Builder builder) {
        if (configFile == null) {
            return;
        }
        try(InputStream inputStream = new FileInputStream(configFile)) {
            Properties properties = new Properties();
            properties.load(inputStream);
            properties.forEach( (k, v) -> builder.putConfig(k.toString(), v));
        } catch (IOException ioExc) {
            log.error("Failed to load config from " + configFile, ioExc);
        }
    }

    private static void setConfigFromEnvironment(KafkaGitopsConfig.Builder builder) {
        Map<String, Object> config = new HashMap<>();
        AtomicReference<String> username = new AtomicReference<>();
        AtomicReference<String> password = new AtomicReference<>();

        Map<String, String> environment = System.getenv();

        environment.forEach((key, value) -> {
            if (key.equals("KAFKA_SASL_JAAS_USERNAME")) {
                username.set(value);
            } else if (key.equals("KAFKA_SASL_JAAS_PASSWORD")) {
                password.set(value);
            } else if (key.startsWith("KAFKA_")) {
                String newKey = key.substring(6).replace("_", ".").toLowerCase();
                config.put(newKey, value);
            }
        });

        handleDefaultConfig(config);
        handleAuthentication(username, password, config);

        log.info("Kafka Config: {}", config);

        builder.putAllConfig(config);
    }

    private static void handleDefaultConfig(Map<String, Object> config) {
        if (!config.containsKey(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)) {
            config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        }

        if (!config.containsKey(CommonClientConfigs.CLIENT_ID_CONFIG)) {
            config.put(CommonClientConfigs.CLIENT_ID_CONFIG, "kafka-gitops");
        }
    }

    private static void handleAuthentication(AtomicReference<String> username, AtomicReference<String> password, Map<String, Object> config) {
        if (username.get() != null && password.get() != null) {
            // Do we need the Plain or SCRAM module?
            String loginModule = null;
            if (config.get(SaslConfigs.SASL_MECHANISM).equals("PLAIN")) {
                loginModule = "org.apache.kafka.common.security.plain.PlainLoginModule";
            } else if (config.get(SaslConfigs.SASL_MECHANISM).equals("SCRAM-SHA-256") || config.get(SaslConfigs.SASL_MECHANISM).equals("SCRAM-SHA-512")) {
                loginModule = "org.apache.kafka.common.security.scram.ScramLoginModule";
            } else {
                throw new MissingConfigurationException("KAFKA_SASL_MECHANISM");
            }

            String value = String.format("%s required username=\"%s\" password=\"%s\";",
                    loginModule, escape(username.get()), escape(password.get()));
            config.put(SaslConfigs.SASL_JAAS_CONFIG, value);
        } else if (username.get() != null) {
            throw new MissingConfigurationException("KAFKA_SASL_JAAS_PASSWORD");
        } else if (password.get() != null) {
            throw new MissingConfigurationException("KAFKA_SASL_JAAS_USERNAME");
        }
    }

    private static String escape(String value) {
        if (value != null) {
            return value.replace("\"", "\\\"");
        }
        return null;
    }
}
