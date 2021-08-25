package com.devshawn.kafka.gitops.domain.state;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;


@FreeBuilder
@JsonDeserialize(builder = ReferenceDetails.Builder.class)
public interface ReferenceDetails {

    String getName();

    String getSubject();

    Integer getVersion();

    class Builder extends ReferenceDetails_Builder {
    }
}
