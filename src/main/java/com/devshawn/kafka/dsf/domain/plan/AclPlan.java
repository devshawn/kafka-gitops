package com.devshawn.kafka.dsf.domain.plan;

import com.devshawn.kafka.dsf.domain.state.AclDetails;
import com.devshawn.kafka.dsf.enums.PlanAction;
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
