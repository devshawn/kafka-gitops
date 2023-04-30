package com.devshawn.kafka.gitops.domain.state;

import org.inferred.freebuilder.FreeBuilder;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;

@FreeBuilder
@JsonDeserialize(builder = ReferenceDetails.Builder.class)
public interface ReferenceDetails {

    String getName();

    String getSubject();

    Integer getVersion();

    public default SchemaReference toSchemaReference() {
        return new SchemaReference(getName(), getSubject(), getVersion());
    }

    class Builder extends ReferenceDetails_Builder {
    }
}
