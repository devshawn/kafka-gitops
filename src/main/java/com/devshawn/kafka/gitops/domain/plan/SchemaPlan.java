package com.devshawn.kafka.gitops.domain.plan;

import com.devshawn.kafka.gitops.domain.state.SchemaDetails;
import com.devshawn.kafka.gitops.enums.PlanAction;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = SchemaPlan.Builder.class)
public interface SchemaPlan {

    String getName();

    PlanAction getAction();

    Optional<SchemaDetails> getSchemaDetails();

    class Builder extends SchemaPlan_Builder {
    }
}
