package com.devshawn.kafka.gitops.domain.confluent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

@FreeBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = ServiceAccount.Builder.class)
public interface ServiceAccount {

    String getId();

    String getName();

    class Builder extends ServiceAccount_Builder {
    }
}
