package com.devshawn.kafka.gitops.domain.plan;

import com.devshawn.kafka.gitops.domain.state.AclDetails;
import com.devshawn.kafka.gitops.enums.PlanAction;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

@FreeBuilder
@JsonDeserialize(builder = AclPlan.Builder.class)
public interface AclPlan {

    String getName();

    AclDetails getAclDetails();

    PlanAction getAction();

    class Builder extends AclPlan_Builder {
    }
}
