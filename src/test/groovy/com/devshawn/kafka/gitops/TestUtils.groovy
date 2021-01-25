package com.devshawn.kafka.gitops

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.acl.*
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.resource.PatternType
import org.apache.kafka.common.resource.ResourcePattern
import org.apache.kafka.common.resource.ResourcePatternFilter
import org.apache.kafka.common.resource.ResourceType
import spock.util.concurrent.PollingConditions

import java.nio.file.Paths

class TestUtils {

    static String getFileContent(String fileName) {
        File file = new File(fileName)
        return file.text
    }

    static String getResourceFileContent(String fileName) {
        URL res = TestUtils.getClassLoader().getResource(fileName)
        File file = Paths.get(res.toURI()).toFile()
        return file.text
    }

    static String getResourceFilePath(String fileName) {
        URL res = TestUtils.getClassLoader().getResource(fileName)
        File file = Paths.get(res.toURI()).toFile()
        return file.getAbsolutePath()
    }

    static void cleanUpCluster() {
        def conditions = new PollingConditions(timeout: 60, initialDelay: 2, factor: 1.25)

        try {
            AdminClient adminClient = AdminClient.create(getKafkaConfig())
            Set<String> topics = adminClient.listTopics().names().get()
            adminClient.deleteTopics(topics).all().get()
            
            conditions.eventually {
                Set<String> remainingTopics = adminClient.listTopics().names().get()
                assert remainingTopics.size() == 0
            }
            
            AclBindingFilter filter = getWildcardFilter()
            adminClient.deleteAcls(Collections.singletonList(filter))
            conditions.eventually {
                List<AclBinding> acls = new ArrayList<>(adminClient.describeAcls(filter).values().get())
                assert acls.size() == 0
            }
            println "Finished cleaning up cluster"
        } catch (Exception ex) {
            println "Error cleaning up kafka cluster"
            println ex
        }

    }

    static void seedCluster() {
        def conditions = new PollingConditions(timeout: 60, initialDelay: 2, factor: 1.25)

        try {
            AdminClient adminClient = AdminClient.create(getKafkaConfig())
            createTopic("delete-topic", 1, adminClient)
            createTopic("test-topic", 1, adminClient)
            createTopic("topic-with-configs-1", 3, adminClient, ["cleanup.policy": "compact", "segment.bytes": "100000"])
            createTopic("topic-with-configs-2", 6, adminClient, ["retention.ms": "60000"])
            createAcl(adminClient)

            conditions.eventually {
                Set<String> newTopics = adminClient.listTopics().names().get()
                assert newTopics.size() == 4

                List<AclBinding> newAcls = new ArrayList<>(adminClient.describeAcls(getWildcardFilter()).values().get())
                assert newAcls.size() == 1
            }
            println "Finished seeding kafka cluster"
        } catch (Exception ex) {
            println "Error seeding up kafka cluster"
            ex.printStackTrace()
        }
    }

    static void createTopic(String name, int partitions, AdminClient adminClient) {
        createTopic(name, partitions, adminClient, null)
    }

    static void createTopic(String name, int partitions, AdminClient adminClient, Map<String, String> configs) {
        NewTopic newTopic = new NewTopic(name, partitions, (short) 2)
        if (configs != null) {
            newTopic.configs(configs)
        }
        adminClient.createTopics(Collections.singletonList(newTopic)).all().get()
    }

    static void createAcl(AdminClient adminClient) {
        ResourcePattern resourcePattern = new ResourcePattern(ResourceType.TOPIC, "test-topic", PatternType.LITERAL)
        AccessControlEntry accessControlEntry = new AccessControlEntry("User:test", "*", AclOperation.READ, AclPermissionType.ALLOW)
        AclBinding aclBinding = new AclBinding(resourcePattern, accessControlEntry)
        adminClient.createAcls(Collections.singletonList(aclBinding)).all().get()
    }

    static AclBindingFilter getWildcardFilter() {
        ResourcePatternFilter resourcePatternFilter = new ResourcePatternFilter(ResourceType.ANY, null, PatternType.ANY)
        AccessControlEntryFilter accessFilter = new AccessControlEntryFilter(null, null, AclOperation.ANY, AclPermissionType.ANY)
        return new AclBindingFilter(resourcePatternFilter, accessFilter)
    }

    static Map<String, Object> getKafkaConfig() {
        String jaasConfig = String.format("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                System.getenv("KAFKA_SASL_JAAS_USERNAME"), System.getenv("KAFKA_SASL_JAAS_PASSWORD"))
        return [
                (CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG): System.getenv("KAFKA_BOOTSTRAP_SERVERS"),
                (CommonClientConfigs.SECURITY_PROTOCOL_CONFIG): System.getenv("KAFKA_SECURITY_PROTOCOL"),
                (SaslConfigs.SASL_MECHANISM)                  : System.getenv("KAFKA_SASL_MECHANISM"),
                (SaslConfigs.SASL_JAAS_CONFIG)                : jaasConfig,
        ]
    }
}
