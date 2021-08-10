package com.devshawn.kafka.gitops.domain.state;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.inferred.freebuilder.FreeBuilder;
import com.devshawn.kafka.gitops.config.SchemaRegistryConfigLoader;
import com.devshawn.kafka.gitops.enums.SchemaCompatibility;
import com.devshawn.kafka.gitops.enums.SchemaType;
import com.devshawn.kafka.gitops.exception.SchemaRegistryExecutionException;
import com.devshawn.kafka.gitops.exception.ValidationException;
import com.devshawn.kafka.gitops.service.SchemaRegistryService;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.confluent.kafka.schemaregistry.AbstractSchemaProvider;
import io.confluent.kafka.schemaregistry.ParsedSchema;

@FreeBuilder
@JsonDeserialize(builder = SchemaDetails.Builder.class) 
public interface SchemaDetails {

    SchemaType getType();

    String getSchema();

    Optional<String> getFile();

    Optional<SchemaCompatibility> getCompatibility();

    List<ReferenceDetails> getReferences();

    class Builder extends SchemaDetails_Builder {
      @Override
      public SchemaDetails build() {
          AbstractSchemaProvider schemaProvider = SchemaRegistryService.schemaProviderFromType(super.getType());
          ParsedSchema parsedSchema;
          if(super.getFile().isPresent()) {
              boolean schema = true;
              try {
                  super.getSchema();
              }catch (IllegalStateException e) {
                  schema = false;
              }
              if ( schema ) {
                  throw new IllegalStateException("schema and file fields cannot be both set at the same time");
              }
              parsedSchema = schemaProvider.parseSchema(loadSchemaFromDisk(super.getFile().get()), Collections.emptyList()).get();
              super.setFile(Optional.empty());
          } else {
              String schema;
              try {
                  schema = super.getSchema();
              }catch (IllegalStateException e) {
                  throw new IllegalStateException("schema or file field must be provided");
              }
              parsedSchema = schemaProvider.parseSchema(schema, Collections.emptyList()).get();
          }
          super.setSchema(parsedSchema.canonicalString());
          return super.build();
      }

      private String loadSchemaFromDisk(String fileName) {
          Map<String, Object> config = SchemaRegistryConfigLoader.load().getConfig();
          final String SCHEMA_DIRECTORY = config.get(SchemaRegistryConfigLoader.SCHEMA_DIRECTORY_KEY).toString();
          if (!Files.exists(Paths.get(SCHEMA_DIRECTORY + "/" + fileName))) {
              throw new ValidationException(String.format("Schema file %s not found in schema directory at %s", getFile(), config.get("SCHEMA_DIRECTORY")));
          }
          try {
              return new String(Files.readAllBytes(Paths.get(SCHEMA_DIRECTORY + "/" + fileName)), StandardCharsets.UTF_8);
          } catch (IOException ex) {
              throw new SchemaRegistryExecutionException("Error thrown when attempting to load a schema from schema directory", ex.getMessage());
          }
      }
    }
}
