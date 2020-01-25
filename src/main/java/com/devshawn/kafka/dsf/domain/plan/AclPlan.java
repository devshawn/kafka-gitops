package com.devshawn.kafka.dsf.domain.plan;

import com.devshawn.kafka.dsf.domain.state.AclDetails;
import com.devshawn.kafka.dsf.enums.PlanAction;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.kafka.common.acl.AclBinding;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = AclPlan.Builder.class)
public interface AclPlan {

    String getName();

    Optional<AclDetails> getAclDetails();

    Optional<AclBinding> getAclBinding();

    PlanAction getAction();

    class Builder extends AclPlan_Builder {
    }
}
