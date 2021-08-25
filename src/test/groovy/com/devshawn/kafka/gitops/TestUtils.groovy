package com.devshawn.kafka.gitops

import java.nio.file.Paths
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.resource.PatternType
import org.apache.kafka.common.resource.ResourcePattern
import org.apache.kafka.common.resource.ResourcePatternFilter
import org.apache.kafka.common.resource.ResourceType
import com.devshawn.kafka.gitops.config.SchemaRegistryConfigLoader
import com.devshawn.kafka.gitops.enums.SchemaCompatibility
import com.devshawn.kafka.gitops.enums.SchemaType
import com.devshawn.kafka.gitops.service.SchemaRegistryService
import io.confluent.kafka.schemaregistry.AbstractSchemaProvider
import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.RestService
import spock.util.concurrent.PollingConditions

class TestUtils {

    private TestUtils() {
    }

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

    static void cleanUpSchemaRegistry() {
        try {
            CachedSchemaRegistryClient schemaRegistryClient = getSchemaRegistryClient();

            Collection<String> subjects = schemaRegistryClient.getAllSubjects();
            for (subject in subjects) {
                schemaRegistryClient.deleteSubject(subject);
                schemaRegistryClient.deleteSubject(subject, true);
            }
            assert schemaRegistryClient.getAllSubjects().size() == 0
            println "Finished cleaning up schema registry"
        } catch (Exception ex) {
            println "Error cleaning up schema registry"
            throw new RuntimeException("Error cleaning up schema registry", ex)
        }
    }

    static void seedSchemaRegistry() {
        try {
            CachedSchemaRegistryClient schemaRegistryClient = getSchemaRegistryClient();
            createSchema("schema-1-json", SchemaType.JSON, 
                "{\"type\":\"object\",\"properties\":{\"f1\":{\"type\":\"string\"}}, \"additionalProperties\": false}", schemaRegistryClient, SchemaCompatibility.BACKWARD)
            createSchema("schema-2-avro", SchemaType.AVRO,
                "{\"type\":\"record\",\"name\":\"TestRecord\",\"namespace\":\"com.devshawn.kafka.gitops\",\"fields\":[{\"name\":\"hello\",\"type\":\"string\"}]}",
                 schemaRegistryClient, SchemaCompatibility.BACKWARD)
            createSchema("schema-3-protobuf", SchemaType.PROTOBUF,
               "syntax = \"proto3\";\npackage com.acme;\n\nmessage OtherRecord {\n  int32 an_id = 1;\n}\n",
                schemaRegistryClient, SchemaCompatibility.FULL)
  
            println "Finished seeding schema registry"
        } catch (Exception ex) {
            println "Error seeding up Schema registry"
            throw new RuntimeException("Error seeding up schema registry", ex)
        }
    }
    
    static void cleanUpAll() {
        cleanUpKafkaCluster();
        cleanUpSchemaRegistry()
    }

    static void cleanUpKafkaCluster() {
        def conditions = new PollingConditions(timeout: 120, initialDelay: 2, factor: 1.25)

        try {
            AdminClient adminClient = AdminClient.create(getKafkaConfig())
            Set<String> topics = adminClient.listTopics().names().get();
            // Do not remove the schema registry topic
            topics.remove("_schemas");
            adminClient.deleteTopics(topics).all().get()
            
            conditions.eventually {
                Set<String> remainingTopics = adminClient.listTopics().names().get()
                assert remainingTopics.size() == 1
                assert remainingTopics.getAt(0).equals("_schemas")
            }
            
            AclBindingFilter filter = getWildcardFilter()
            adminClient.deleteAcls(Collections.singletonList(filter))
            conditions.eventually {
                List<AclBinding> acls = new ArrayList<>(adminClient.describeAcls(filter).values().get())
                assert acls.size() == 0
            }
            println "Finished cleaning up kafka cluster"
        } catch (Exception ex) {
            println "Error cleaning up kafka cluster"
            throw new RuntimeException("Error cleaning up kafka cluster", ex)
        }

    }

    static void seedKafkaCluster() {
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
                assert newTopics.size() == 5

                List<AclBinding> newAcls = new ArrayList<>(adminClient.describeAcls(getWildcardFilter()).values().get())
                assert newAcls.size() == 1
            }
            println "Finished seeding kafka cluster"
        } catch (Exception ex) {
            println "Error seeding up kafka cluster"
            throw new RuntimeException("Error seeding up kafka cluster", ex)
        }
    }

    static void createSchema(String subject, SchemaType type, String schema , SchemaRegistryClient client, SchemaCompatibility compatibility) {
        AbstractSchemaProvider schemaProvider = SchemaRegistryService.schemaProviderFromType(type);
        ParsedSchema parsedSchema = schemaProvider.parseSchema(schema, Collections.emptyList()).get();
        createSchema(subject, type, parsedSchema, client, compatibility)
    }

    static void createSchema(String subject, SchemaType type, ParsedSchema schema , SchemaRegistryClient client, SchemaCompatibility compatibility) {
        CachedSchemaRegistryClient schemaRegistryClient = getSchemaRegistryClient();
        int id = schemaRegistryClient.register(subject, schema);
        String compat = schemaRegistryClient.updateCompatibility(subject, compatibility.toString());
        println "Schema subject '" + subject + "' with id " + id + " created (compatibility: " + compat + ")"
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

    static CachedSchemaRegistryClient getSchemaRegistryClient() {
          Map<String, Object> config = SchemaRegistryConfigLoader.load().getConfig();
          RestService restService = new RestService(config.get(SchemaRegistryConfigLoader.SCHEMA_REGISTRY_URL_KEY).toString())
        return  new CachedSchemaRegistryClient(restService, 10, config);
    }
}
