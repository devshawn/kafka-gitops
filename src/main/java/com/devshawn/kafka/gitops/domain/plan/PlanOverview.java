package com.devshawn.kafka.gitops.domain.plan;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

@FreeBuilder
@JsonDeserialize(builder = PlanOverview.Builder.class)
public interface PlanOverview {

    Long getAdd();

    Long getRemove();

    Long getUpdate();

    Long getNoChange();

    class Builder extends PlanOverview_Builder {
    }
}
