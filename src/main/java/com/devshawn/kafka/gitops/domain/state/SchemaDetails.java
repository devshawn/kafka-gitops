package com.devshawn.kafka.gitops.domain.state;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.List;

@FreeBuilder
@JsonDeserialize(builder = SchemaDetails.Builder.class)
public interface SchemaDetails {

    String getType();

    String getFile();

    List<String> getSubjects();

    List<ReferenceDetails> getReferences();

    class Builder extends SchemaDetails_Builder {
    }
}
