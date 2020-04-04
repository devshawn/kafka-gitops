package com.devshawn.kafka.gitops.domain.plan;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.List;

@FreeBuilder
@JsonDeserialize(builder = DesiredPlan.Builder.class)
public interface DesiredPlan {

    List<TopicPlan> getTopicPlans();

    List<AclPlan> getAclPlans();

    class Builder extends DesiredPlan_Builder {
    }
}
