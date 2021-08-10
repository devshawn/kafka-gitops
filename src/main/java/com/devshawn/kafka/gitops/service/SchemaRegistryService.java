package com.devshawn.kafka.gitops.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.kafka.common.config.SaslConfigs;
import com.devshawn.kafka.gitops.config.SchemaRegistryConfig;
import com.devshawn.kafka.gitops.config.SchemaRegistryConfigLoader;
import com.devshawn.kafka.gitops.domain.plan.SchemaPlan;
import com.devshawn.kafka.gitops.domain.state.SchemaDetails;
import com.devshawn.kafka.gitops.enums.SchemaCompatibility;
import com.devshawn.kafka.gitops.enums.SchemaType;
import com.devshawn.kafka.gitops.exception.SchemaRegistryExecutionException;
import com.devshawn.kafka.gitops.exception.ValidationException;
import io.confluent.kafka.schemaregistry.AbstractSchemaProvider;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.SchemaProvider;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.client.security.basicauth.SaslBasicAuthCredentialProvider;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;

public class SchemaRegistryService {

    private final SchemaRegistryConfig config;

    public SchemaRegistryService(SchemaRegistryConfig config) {
        this.config = config;
    }

    public Map<String, Object> getConfig() {
      return Collections.unmodifiableMap(config.getConfig());
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
        AbstractSchemaProvider schemaProvider = schemaProviderFromType(schemaPlan.getSchemaDetails().get().getType());
        ParsedSchema parsedSchema = parseSchema(schemaPlan.getName(), schemaPlan.getSchemaDetails().get(), schemaProvider, cachedSchemaRegistryClient);
        int id;
        try {
            id = cachedSchemaRegistryClient.register(schemaPlan.getName(), parsedSchema);
        } catch (IOException | RestClientException ex) {
            throw new SchemaRegistryExecutionException("Error thrown when attempting to register subject '" + schemaPlan.getName() + "' in schema registry", ex.getMessage());
        }
        try {
            cachedSchemaRegistryClient.updateCompatibility(schemaPlan.getName(), schemaPlan.getSchemaDetails().get().getCompatibility().get().toString());
        } catch (IOException | RestClientException ex) {
            throw new SchemaRegistryExecutionException("Error thrown when attempting to update compatibility of the newly registered subject '" + schemaPlan.getName() + "' in schema registry", ex.getMessage());
        }
        return id;
    }

    public static AbstractSchemaProvider schemaProviderFromType(SchemaType schemaType) {
        AbstractSchemaProvider schemaProvider;
        if (schemaType == SchemaType.AVRO) {
            schemaProvider = new AvroSchemaProvider();
        } else if (schemaType == SchemaType.JSON) {
            schemaProvider = new JsonSchemaProvider();
        } else if (schemaType == SchemaType.PROTOBUF) {
            schemaProvider = new ProtobufSchemaProvider();
        } else {
            throw new ValidationException("Unknown schema type: " + schemaType);
        }
        return schemaProvider;
    }

    public void validateSchema(String subject, SchemaDetails schemaDetails) {
        AbstractSchemaProvider schemaProvider = schemaProviderFromType(schemaDetails.getType());
        validateSchema(subject, schemaDetails, schemaProvider);
    }

    public void validateSchema(String subject, SchemaDetails schemaDetails, AbstractSchemaProvider schemaProvider) {
        CachedSchemaRegistryClient schemaRegistryClient = createSchemaRegistryClient();
        ParsedSchema parsedSchema =  parseSchema(subject, schemaDetails, schemaProvider, schemaRegistryClient);
        try {
            List<String> differences = schemaRegistryClient.testCompatibilityVerbose(subject, parsedSchema);
            if(! differences.isEmpty()) {
                throw new ValidationException(String.format("%s schema '%s' is not compatible with the latest one: %s", schemaProvider.schemaType(), subject, differences));
            }
        } catch (IOException | RestClientException ex) {
            throw new ValidationException(String.format("Error thrown when attempting to check the compatibility of the new schema for '%s': %s", subject, ex.getMessage()));
        }
    }

    private ParsedSchema parseSchema(String subject, SchemaDetails schemaDetails, AbstractSchemaProvider schemaProvider, CachedSchemaRegistryClient schemaRegistryClient) {
        Optional<ParsedSchema> parsedSchema;
        if (schemaDetails.getReferences().isEmpty()) {
            parsedSchema = schemaProvider.parseSchema(schemaDetails.getSchema(), Collections.emptyList());
            if (!parsedSchema.isPresent()) {
                throw new ValidationException(String.format("%s schema for subject '%s' could not be parsed.", schemaProvider.schemaType(), subject));
            }
        } else {
            List<SchemaReference> schemaReferences = new ArrayList<>();
            schemaDetails.getReferences().forEach(referenceDetails -> {
                SchemaReference schemaReference = new SchemaReference(referenceDetails.getName(), referenceDetails.getSubject(), referenceDetails.getVersion());
                schemaReferences.add(schemaReference);
            });
            // we need to pass a schema registry client as a config because the underlying code validates against the current state
            schemaProvider.configure(Collections.singletonMap(SchemaProvider.SCHEMA_VERSION_FETCHER_CONFIG, schemaRegistryClient));
            try {
                parsedSchema = schemaProvider.parseSchema(schemaDetails.getSchema(), schemaReferences);
            } catch (IllegalStateException ex) {
                throw new ValidationException(String.format("Reference validation error: %s", ex.getMessage()));
            } catch (RuntimeException ex) {
                throw new ValidationException(String.format("Error thrown when attempting to validate %s schema with reference: %s", subject, ex.getMessage()));
            }
            if (!parsedSchema.isPresent()) {
                throw new ValidationException(String.format("%s referenced schema could not be parsed for subject %s", schemaProvider.schemaType(), subject));
            }
        }
        return parsedSchema.get();
    }

    public SchemaMetadata getLatestSchemaMetadata(String subject) {
        final CachedSchemaRegistryClient cachedSchemaRegistryClient = createSchemaRegistryClient();
        try {
            return cachedSchemaRegistryClient.getLatestSchemaMetadata(subject);
        } catch (IOException | RestClientException ex) {
            throw new SchemaRegistryExecutionException("Error thrown when attempting to get schema metadata for subject '" + subject + "'", ex.getMessage());
        }
    }

    public SchemaCompatibility getGlobalSchemaCompatibility() {
        final CachedSchemaRegistryClient cachedSchemaRegistryClient = createSchemaRegistryClient();
        try {
            return SchemaCompatibility.valueOf(cachedSchemaRegistryClient.getCompatibility(null));
        } catch (IOException | RestClientException ex) {
            throw new SchemaRegistryExecutionException("Error thrown when attempting to get global schema compatibility", ex.getMessage());
        }
    }
    public SchemaCompatibility getSchemaCompatibility(String subject, SchemaCompatibility globalCompatibility) {
        final CachedSchemaRegistryClient cachedSchemaRegistryClient = createSchemaRegistryClient();
        try {
            return SchemaCompatibility.valueOf(cachedSchemaRegistryClient.getCompatibility(subject));
        } catch (IOException ex) {
            throw new SchemaRegistryExecutionException("Error thrown when attempting to get schema compatibility for subject '" + subject + "'", ex.getMessage());
        } catch (RestClientException ex) {
            if(ex.getErrorCode() == 40401) {
                return globalCompatibility;
            }
            throw new SchemaRegistryExecutionException("Error thrown when attempting to get schema compatibility for subject '" + subject + "'", ex.getMessage());
        }
    }

    public boolean deepEquals(SchemaDetails schemaDetails, SchemaMetadata schemaMetadata) {
        AbstractSchemaProvider schemaProvider = schemaProviderFromType(schemaDetails.getType());
        ParsedSchema newSchema = schemaProvider.parseSchema(schemaDetails.getSchema(), Collections.emptyList()).get();
        ParsedSchema previousSchema = schemaProvider.parseSchema(schemaMetadata.getSchema(), Collections.emptyList()).get();
        return (newSchema.deepEquals(previousSchema));
    }

    public CachedSchemaRegistryClient createSchemaRegistryClient() {
        RestService restService = new RestService(config.getConfig().get(SchemaRegistryConfigLoader.SCHEMA_REGISTRY_URL_KEY).toString());
        if(config.getConfig().get(SchemaRegistryConfigLoader.SCHEMA_REGISTRY_SASL_CONFIG_KEY) != null) {
            SaslBasicAuthCredentialProvider saslBasicAuthCredentialProvider = new SaslBasicAuthCredentialProvider();
            Map<String, Object> clientConfig = new HashMap<>();
            clientConfig.put(SaslConfigs.SASL_JAAS_CONFIG, config.getConfig()
                .get(SchemaRegistryConfigLoader.SCHEMA_REGISTRY_SASL_CONFIG_KEY).toString());
            saslBasicAuthCredentialProvider.configure(clientConfig);
            restService.setBasicAuthCredentialProvider(saslBasicAuthCredentialProvider);
        }
        return new CachedSchemaRegistryClient(restService, 10);
    }

}
