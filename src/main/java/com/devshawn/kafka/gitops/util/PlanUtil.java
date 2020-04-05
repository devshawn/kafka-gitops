package com.devshawn.kafka.gitops.util;

import com.devshawn.kafka.gitops.domain.plan.DesiredPlan;
import com.devshawn.kafka.gitops.domain.plan.PlanOverview;
import com.devshawn.kafka.gitops.enums.PlanAction;

import java.util.EnumMap;
import java.util.EnumSet;

public class PlanUtil {

    public static PlanOverview getOverview(DesiredPlan desiredPlan) {
        EnumMap<PlanAction, Long> map = getPlanActionMap();
        desiredPlan.getTopicPlans().forEach(it -> map.put(it.getAction(), map.get(it.getAction()) + 1));
        desiredPlan.getAclPlans().forEach(it -> map.put(it.getAction(), map.get(it.getAction()) + 1));
        return buildPlanOverview(map);
    }

    public static PlanOverview getTopicPlanOverview(DesiredPlan desiredPlan) {
        EnumMap<PlanAction, Long> map = getPlanActionMap();
        desiredPlan.getTopicPlans().forEach(it -> map.put(it.getAction(), map.get(it.getAction()) + 1));
        return buildPlanOverview(map);
    }

    public static PlanOverview getAclPlanOverview(DesiredPlan desiredPlan) {
        EnumMap<PlanAction, Long> map = getPlanActionMap();
        desiredPlan.getAclPlans().forEach(it -> map.put(it.getAction(), map.get(it.getAction()) + 1));
        return buildPlanOverview(map);
    }

    private static EnumMap<PlanAction, Long> getPlanActionMap() {
        EnumMap<PlanAction, Long> map = new EnumMap<>(PlanAction.class);
        EnumSet.allOf(PlanAction.class).forEach(it -> map.putIfAbsent(it, 0L));
        return map;
    }

    private static PlanOverview buildPlanOverview(EnumMap<PlanAction, Long> map) {
        return new PlanOverview.Builder()
                .setAdd(map.get(PlanAction.ADD))
                .setUpdate(map.get(PlanAction.UPDATE))
                .setRemove(map.get(PlanAction.REMOVE))
                .setNoChange(map.get(PlanAction.NO_CHANGE))
                .build();
    }
}
