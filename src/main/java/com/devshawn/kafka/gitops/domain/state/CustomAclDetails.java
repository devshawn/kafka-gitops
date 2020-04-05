package com.devshawn.kafka.gitops.domain.state;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = CustomAclDetails.Builder.class)
public abstract class CustomAclDetails {

    public abstract String getName();

    public abstract String getType();

    public abstract String getPattern();

    public abstract Optional<String> getPrincipal();

    public abstract String getHost();

    public abstract String getOperation();

    public abstract String getPermission();

    public static class Builder extends CustomAclDetails_Builder {

    }
}