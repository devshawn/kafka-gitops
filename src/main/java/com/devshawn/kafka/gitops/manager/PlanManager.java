package com.devshawn.kafka.gitops.manager;

import com.devshawn.kafka.gitops.config.ManagerConfig;
import com.devshawn.kafka.gitops.domain.plan.AclPlan;
import com.devshawn.kafka.gitops.domain.plan.DesiredPlan;
import com.devshawn.kafka.gitops.domain.plan.PlanOverview;
import com.devshawn.kafka.gitops.domain.plan.TopicConfigPlan;
import com.devshawn.kafka.gitops.domain.plan.TopicDetailsPlan;
import com.devshawn.kafka.gitops.domain.plan.TopicPlan;
import com.devshawn.kafka.gitops.domain.state.AclDetails;
import com.devshawn.kafka.gitops.domain.state.DesiredState;
import com.devshawn.kafka.gitops.domain.state.TopicDetails;
import com.devshawn.kafka.gitops.enums.PlanAction;
import com.devshawn.kafka.gitops.exception.PlanIsUpToDateException;
import com.devshawn.kafka.gitops.exception.ReadPlanInputException;
import com.devshawn.kafka.gitops.exception.ValidationException;
import com.devshawn.kafka.gitops.exception.WritePlanOutputException;
import com.devshawn.kafka.gitops.service.KafkaService;
import com.devshawn.kafka.gitops.util.PlanUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.config.ConfigResource;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlanManager {

    private static org.slf4j.Logger log = LoggerFactory.getLogger(PlanManager.class);

    private final ManagerConfig managerConfig;
    private final KafkaService kafkaService;
    private final ObjectMapper objectMapper;

    public PlanManager(ManagerConfig managerConfig, KafkaService kafkaService, ObjectMapper objectMapper) {
        this.managerConfig = managerConfig;
        this.kafkaService = kafkaService;
        this.objectMapper = objectMapper;
    }

    public void planTopics(DesiredState desiredState, DesiredPlan.Builder desiredPlan) {
        Map<String, TopicDescription> topics = kafkaService.getTopics(); 
        List<String> topicNames = topics.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList());
        Map<String, List<ConfigEntry>> topicConfigs = fetchTopicConfigurations(topicNames);

        desiredState.getTopics().forEach((key, value) -> {
            TopicDetailsPlan.Builder topicDetailsPlan = new TopicDetailsPlan.Builder();
            topicDetailsPlan.setPartitionsAction(PlanAction.NO_CHANGE)
              .setReplicationAction(PlanAction.NO_CHANGE);

            TopicPlan.Builder topicPlan = new TopicPlan.Builder()
                .setName(key);
            boolean topicDetailsAddOrUpdate = false;
            if (!topicNames.contains(key)) {
                log.info("[PLAN] Topic {} does not exist; it will be created.", key);
                topicPlan.setAction(PlanAction.ADD);
                topicDetailsPlan.setPartitionsAction(PlanAction.ADD)
                    .setPartitions(value.getPartitions())
                    .setReplicationAction(PlanAction.ADD)
                    .setReplication(value.getReplication().get());
                planTopicConfigurations(key, value, topicConfigs.get(key), topicPlan);
                topicDetailsAddOrUpdate = true;
            } else {
                log.info("[PLAN] Topic {} exists, it will not be created.", key);
                TopicDescription topicDescription = topics.get(key);
                
                topicPlan.setAction(PlanAction.NO_CHANGE);
                topicDetailsPlan.setPartitions(topicDescription.partitions().size())
                    .setReplication(topicDescription.partitions().get(0).replicas().size());

                if (value.getPartitions().intValue() != topicDescription.partitions().size()) {
                    if( value.getPartitions().intValue() < topicDescription.partitions().size()) {
                        throw new ValidationException("Removing the partition number is not supported by Apache Kafka "
                            + "(topic: " + key + " ("+topicDescription.partitions().size()+" -> "+value.getPartitions().intValue()+"))");
                    }
                    topicDetailsPlan.setPartitions(value.getPartitions())
                        .setPreviousPartitions(topicDescription.partitions().size());
                    topicDetailsPlan.setPartitionsAction(PlanAction.UPDATE);
                    topicDetailsAddOrUpdate = true;
                }
                if (value.getReplication().isPresent() && 
                       ( value.getReplication().get().intValue() != topicDescription.partitions().get(0).replicas().size()) ) {
                    topicDetailsPlan.setReplication(value.getReplication().get())
                        .setPreviousReplication(topicDescription.partitions().get(0).replicas().size());
                    topicDetailsPlan.setReplicationAction(PlanAction.UPDATE);
                    topicDetailsAddOrUpdate = true;
                }
                if (topicDetailsAddOrUpdate) {
                    topicPlan.setAction(PlanAction.UPDATE);
                }

                planTopicConfigurations(key, value, topicConfigs.get(key), topicPlan);
            }

            topicPlan.setTopicDetailsPlan(topicDetailsPlan.build());

            desiredPlan.addTopicPlans(topicPlan.build());
        });

        topics.forEach((currentTopicName, currentTopicDescription) -> {
            boolean shouldIgnore = desiredState.getPrefixedTopicsToIgnore().stream().anyMatch(it -> currentTopicName.startsWith(it));
            if (shouldIgnore) {
                log.info("[PLAN] Ignoring topic {} due to prefix", currentTopicName);
                return;
            }

            if (!managerConfig.isDeleteDisabled() && desiredState.getTopics().getOrDefault(currentTopicName, null) == null) {
                TopicPlan topicPlan = new TopicPlan.Builder()
                        .setName(currentTopicName)
                        .setAction(PlanAction.REMOVE)
                        .build();

                desiredPlan.addTopicPlans(topicPlan);
            }
        });
    }

    private void planTopicConfigurations(String topicName, TopicDetails topicDetails, List<ConfigEntry> configs, TopicPlan.Builder topicPlan) {
        Map<String, TopicConfigPlan> configPlans = new HashMap<>();
        List<ConfigEntry> customConfigs = configs == null ? new ArrayList<>() : configs.stream()
                .filter(it -> it.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG)
                .collect(Collectors.toList());

        customConfigs.forEach(currentConfig -> {
            String newConfig = topicDetails.getConfigs().getOrDefault(currentConfig.name(), null);

            TopicConfigPlan.Builder topicConfigPlan = new TopicConfigPlan.Builder()
                    .setKey(currentConfig.name());

            if (currentConfig.value().equals(newConfig)) {
                topicConfigPlan.setAction(PlanAction.NO_CHANGE);
                topicConfigPlan.setValue(currentConfig.value());
                configPlans.put(currentConfig.name(), topicConfigPlan.build());
            } else if (newConfig == null) {
                topicConfigPlan.setAction(PlanAction.REMOVE);
                topicConfigPlan.setPreviousValue(currentConfig.value());
                configPlans.put(currentConfig.name(), topicConfigPlan.build());
                if (topicPlan.getAction() == null || topicPlan.getAction().equals(PlanAction.NO_CHANGE)) {
                  topicPlan.setAction(PlanAction.UPDATE);
                }
            }
        });

        topicDetails.getConfigs().forEach((key, value) -> {
            ConfigEntry currentConfig = customConfigs.stream().filter(it -> it.name().equals(key)).findFirst().orElse(null);

            TopicConfigPlan.Builder topicConfigPlan = new TopicConfigPlan.Builder()
                    .setKey(key)
                    .setValue(value);

            if (currentConfig == null) {
                topicConfigPlan.setAction(PlanAction.ADD);
                configPlans.put(key, topicConfigPlan.build());
                if (topicPlan.getAction() == null || topicPlan.getAction().equals(PlanAction.NO_CHANGE)) {
                  topicPlan.setAction(PlanAction.UPDATE);
                }
            } else if (!currentConfig.value().equals(value)) {
                topicConfigPlan.setPreviousValue(currentConfig.value())
                .setAction(PlanAction.UPDATE);
                configPlans.put(key, topicConfigPlan.build());
                if (topicPlan.getAction() == null || topicPlan.getAction().equals(PlanAction.NO_CHANGE)) {
                  topicPlan.setAction(PlanAction.UPDATE);
                }
            }
        });

        configPlans.forEach((key, plan) -> {
            log.info("[PLAN] Topic {} | [{}] {}", topicName, plan.getAction(), plan.getKey());
            topicPlan.addTopicConfigPlans(plan);
        });
    }

    public void planAcls(DesiredState desiredState, DesiredPlan.Builder desiredPlan) {
        List<AclBinding> currentAcls = kafkaService.getAcls();

        currentAcls.forEach(acl -> {
            Map.Entry<String, AclDetails> detailsEntry = desiredState.getAcls().entrySet().stream()
                    .filter(entry -> entry.getValue().equalsAclBinding(acl))
                    .findFirst().orElse(null);

            AclPlan.Builder aclPlan = new AclPlan.Builder();

            if (detailsEntry != null) {
                aclPlan.setName(detailsEntry.getKey());
                aclPlan.setAclDetails(detailsEntry.getValue());
                aclPlan.setAction(PlanAction.NO_CHANGE);
                desiredPlan.addAclPlans(aclPlan.build());
            } else {
                aclPlan.setName("Unnamed ACL");
                aclPlan.setAclDetails(AclDetails.fromAclBinding(acl));
                aclPlan.setAction(PlanAction.REMOVE);

                if (!managerConfig.isDeleteDisabled()) {
                    desiredPlan.addAclPlans(aclPlan.build());
                }
            }
        });

        desiredState.getAcls().forEach((key, value) -> {
            AclBinding aclBinding = currentAcls.stream().filter(value::equalsAclBinding).findFirst().orElse(null);
            if (aclBinding == null) {
                AclPlan aclPlan = new AclPlan.Builder()
                        .setName(key)
                        .setAclDetails(value)
                        .setAction(PlanAction.ADD)
                        .build();

                desiredPlan.addAclPlans(aclPlan);
            }
        });
    }

    public void validatePlanHasChanges(DesiredPlan desiredPlan, boolean deleteDisabled, boolean skipAclsDisabled) {
        PlanOverview planOverview = PlanUtil.getOverview(desiredPlan, deleteDisabled, skipAclsDisabled);
        if (planOverview.getAdd() == 0 && planOverview.getUpdate() == 0 && planOverview.getRemove() == 0) {
            throw new PlanIsUpToDateException();
        }
    }

    public DesiredPlan readPlanFromFile() {
        if (!managerConfig.getPlanFile().isPresent()) {
            return null;
        }

        try {
            return objectMapper.readValue(managerConfig.getPlanFile().get(), DesiredPlan.class);
        } catch (FileNotFoundException ex) {
            throw new ReadPlanInputException("The specified plan file could not be found.");
        } catch (IOException ex) {
            throw new ReadPlanInputException();
        }
    }

    public void writePlanToFile(DesiredPlan desiredPlan) {
        if (managerConfig.getPlanFile().isPresent()) {
            try {
                managerConfig.getPlanFile().get().createNewFile();
                DesiredPlan outputPlan = managerConfig.isIncludeUnchangedEnabled() ? desiredPlan : desiredPlan.toChangesOnlyPlan();
                FileWriter writer = new FileWriter(managerConfig.getPlanFile().get());
                writer.write(objectMapper.writeValueAsString(outputPlan));
                writer.close();
            } catch (IOException ex) {
                throw new WritePlanOutputException(ex.getMessage() + " ('" + managerConfig.getPlanFile().get() + "')");
            }
        }
    }

    private Map<String, List<ConfigEntry>> fetchTopicConfigurations(List<String> topicNames) {
        Map<String, List<ConfigEntry>> map = new HashMap<>();
        Map<ConfigResource, Config> configs = kafkaService.describeConfigsForTopics(topicNames);
        configs.forEach((key, value) -> map.put(key.name(), new ArrayList<ConfigEntry>(value.entries())));
        return map;
    }
}
