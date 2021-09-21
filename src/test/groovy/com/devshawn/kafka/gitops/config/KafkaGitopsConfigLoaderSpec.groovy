package com.devshawn.kafka.gitops.config

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs
import org.junit.ClassRule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import spock.lang.Shared
import spock.lang.Specification

class KafkaGitopsConfigLoaderSpec extends Specification {

    @Shared
    @ClassRule
    EnvironmentVariables environmentVariables

    void setupSpec() {
        environmentVariables.set("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
        environmentVariables.set("KAFKA_SASL_MECHANISM", "PLAIN")
        environmentVariables.set("KAFKA_SECURITY_PROTOCOL", "SASL_PLAINTEXT")
    }

    void 'test username and password shortcut'() {
        setup:
        environmentVariables.set("KAFKA_SASL_JAAS_USERNAME", "test")
        environmentVariables.set("KAFKA_SASL_JAAS_PASSWORD", "test-secret")

        when:
        KafkaGitopsConfig config = KafkaGitopsConfigLoader.load()

        then:
        config
        config.config.get(SaslConfigs.SASL_JAAS_CONFIG) == "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"test\" password=\"test-secret\";"
    }

    void 'test escaping username and password shortcut'() {
        setup:
        environmentVariables.set("KAFKA_SASL_JAAS_USERNAME", "te\"st")
        environmentVariables.set("KAFKA_SASL_JAAS_PASSWORD", "te\"st-secr\"et")

        when:
        KafkaGitopsConfig config = KafkaGitopsConfigLoader.load()

        then:
        config
        config.config.get(SaslConfigs.SASL_JAAS_CONFIG) == "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"te\\\"st\" password=\"te\\\"st-secr\\\"et\";"
    }

    void 'test command config file'() {
        setup:
        File configFile = new File(getClass().getResource("/command.properties").toURI())

        when:
        KafkaGitopsConfig config = KafkaGitopsConfigLoader.load(configFile)

        then:
        config.config.get(CommonClientConfigs.CLIENT_ID_CONFIG) == "kafka-gitops"
        config.config.get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG) == "localhost:9092"
        config.config.get(SaslConfigs.SASL_MECHANISM) == "PLAIN"
    }

}
