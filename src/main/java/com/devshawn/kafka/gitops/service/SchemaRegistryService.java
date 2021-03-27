package com.devshawn.kafka.gitops.service;


import com.devshawn.kafka.gitops.config.SchemaRegistryConfig;
import com.devshawn.kafka.gitops.domain.plan.SchemaPlan;
import com.devshawn.kafka.gitops.exception.SchemaRegistryExecutionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.client.security.basicauth.SaslBasicAuthCredentialProvider;
import org.apache.kafka.common.config.SaslConfigs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SchemaRegistryService {

    private final SchemaRegistryConfig config;

    public SchemaRegistryService(SchemaRegistryConfig config) {
        this.config = config;
    }

    public List<String> getAllSubjects() {
        final CachedSchemaRegistryClient cachedSchemaRegistryClient = createSchemaRegistryClient();
        try {
            return new ArrayList<>(cachedSchemaRegistryClient.getAllSubjects());
        } catch (IOException | RestClientException ex) {
            throw new SchemaRegistryExecutionException("Error thrown when attempting to get all schema registry subjects", ex.getMessage());
        }
    }

    public void deleteSubject(String subject, boolean isPermanent) {
        final CachedSchemaRegistryClient cachedSchemaRegistryClient = createSchemaRegistryClient();
        try {
            // must always soft-delete
            cachedSchemaRegistryClient.deleteSubject(subject);
            if (isPermanent) {
                cachedSchemaRegistryClient.deleteSubject(subject, true);
            }
        } catch (IOException | RestClientException ex) {
            throw new SchemaRegistryExecutionException("Error thrown when attempting to get delete subject from schema registry", ex.getMessage());
        }
    }

    public int register(SchemaPlan schemaPlan) {
        final CachedSchemaRegistryClient cachedSchemaRegistryClient = createSchemaRegistryClient();
        AvroSchemaProvider avroSchemaProvider = new AvroSchemaProvider();
        Optional<ParsedSchema> parsedSchema = avroSchemaProvider.parseSchema(loadSchemaFromDisk(schemaPlan.getSchemaDetails().get().getFile()), Collections.emptyList());
        try {
            return cachedSchemaRegistryClient.register(schemaPlan.getName(), parsedSchema.get());
        } catch (IOException | RestClientException ex) {
            throw new SchemaRegistryExecutionException("Error thrown when attempting to register subject with schema registry", ex.getMessage());
        }
    }

    public SchemaMetadata getLatestSchemaMetadata(String subject) {
        final CachedSchemaRegistryClient cachedSchemaRegistryClient = createSchemaRegistryClient();
        try {
            return cachedSchemaRegistryClient.getLatestSchemaMetadata(subject);
        } catch (IOException | RestClientException ex) {
            throw new SchemaRegistryExecutionException("Error thrown when attempting to get delete subject from schema registry", ex.getMessage());
        }
    }

    public String compareSchemasAndReturnDiff(String schemaStringOne, String schemaStringTwo) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode schemaOne = objectMapper.readTree(schemaStringOne);
            JsonNode schemaTwo = objectMapper.readTree(schemaStringTwo);
            JsonNode diff = JsonDiff.asJson(schemaOne, schemaTwo);
            if (diff.isEmpty()) {
                return null;
            }
            return diff.toString();
        } catch (JsonProcessingException ex) {
            throw new SchemaRegistryExecutionException("Error thrown when attempting to compare the schemas", ex.getMessage());
        }
    }

    public String loadSchemaFromDisk(String fileName) {
        final String SCHEMA_DIRECTORY = config.getConfig().get("SCHEMA_DIRECTORY").toString();
        try {
            return new String(Files.readAllBytes(Paths.get(SCHEMA_DIRECTORY + "/" + fileName)), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new SchemaRegistryExecutionException("Error thrown when attempting to load a schema from schema directory", ex.getMessage());
        }
    }

    public CachedSchemaRegistryClient createSchemaRegistryClient() {
        RestService restService = new RestService(config.getConfig().get("SCHEMA_REGISTRY_URL").toString());
        SaslBasicAuthCredentialProvider saslBasicAuthCredentialProvider = new SaslBasicAuthCredentialProvider();
        Map<String, Object> clientConfig = new HashMap<>();
        clientConfig.put(SaslConfigs.SASL_JAAS_CONFIG, config.getConfig().get("SCHEMA_REGISTRY_SASL_CONFIG").toString());
        saslBasicAuthCredentialProvider.configure(clientConfig);
        restService.setBasicAuthCredentialProvider(saslBasicAuthCredentialProvider);
        return new CachedSchemaRegistryClient(restService, 10);
    }

}
