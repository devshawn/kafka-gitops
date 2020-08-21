package com.devshawn.kafka.gitops.domain.options;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

@FreeBuilder
@JsonDeserialize(builder = GetAclOptions.Builder.class)
public interface GetAclOptions {

    String getServiceName();

    Boolean getDescribeAclEnabled();

    class Builder extends GetAclOptions_Builder {
    }
}
