package com.devshawn.kafka.dsf;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.devshawn.kafka.dsf.config.KafkaDsfConfig;
import com.devshawn.kafka.dsf.config.KafkaDsfConfigLoader;
import com.devshawn.kafka.dsf.domain.plan.*;
import com.devshawn.kafka.dsf.domain.state.AclDetails;
import com.devshawn.kafka.dsf.domain.state.DesiredState;
import com.devshawn.kafka.dsf.domain.state.TopicDetails;
import com.devshawn.kafka.dsf.enums.PlanAction;
import com.devshawn.kafka.dsf.service.KafkaService;
import com.devshawn.kafka.dsf.service.ParserService;
import com.devshawn.kafka.dsf.util.LogUtil;
import com.devshawn.kafka.dsf.util.PlanUtil;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.config.ConfigResource;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class StateManager {

    private static org.slf4j.Logger log = LoggerFactory.getLogger(StateManager.class);

    private final ParserService parserService;
    private final KafkaDsfConfig config;
    private final KafkaService kafkaService;

    public StateManager(boolean verbose, File file) {
        initializeLogger(verbose);
        this.config = KafkaDsfConfigLoader.load();
        this.kafkaService = new KafkaService(config);
        this.parserService = new ParserService(file);
    }

    public void validate() {
        parserService.parseStateFile();
    }

    public void validatePlanHasChanges(DesiredPlan desiredPlan) {
        PlanOverview planOverview = PlanUtil.getOverview(desiredPlan);
        if (planOverview.getAdd() == 0 && planOverview.getUpdate() == 0 && planOverview.getRemove() == 0) {
            LogUtil.printNoChangesMessage();
            System.exit(0);
        }
    }

    public DesiredPlan plan() {
        return generatePlan();
    }

    public DesiredPlan apply() {
        DesiredPlan desiredPlan = generatePlan();
        validatePlanHasChanges(desiredPlan);

        applyTopics(desiredPlan);
        applyAcls(desiredPlan);

        return desiredPlan;
    }

    private DesiredPlan generatePlan() {
        DesiredState desiredState = parserService.parseStateFile();
        DesiredPlan.Builder desiredPlan = new DesiredPlan.Builder();

        planTopics(desiredState, desiredPlan);
        planAcls(desiredState, desiredPlan);

        return desiredPlan.build();
    }

    private void planTopics(DesiredState desiredState, DesiredPlan.Builder desiredPlan) {
        List<TopicListing> topics = kafkaService.getTopics();

        desiredState.getTopics().forEach((key, value) -> {
            TopicPlan.Builder topicPlan = new TopicPlan.Builder()
                    .setName(key)
                    .setTopicDetails(value);

            TopicDescription topicDescription = kafkaService.describeTopic(key);

            if (topicDescription == null) {
                log.info("[PLAN] Topic {} does not exist; it will be created.", key);
                topicPlan.setAction(PlanAction.ADD);
            } else {
                log.info("[PLAN] Topic {} exists, it will not be created.", key);
                topicPlan.setAction(PlanAction.NO_CHANGE);
                planTopicConfigurations(key, value, topicPlan);
            }

            desiredPlan.addTopicPlans(topicPlan.build());
        });

        topics.forEach(currentTopic -> {
            if (desiredState.getTopics().getOrDefault(currentTopic.name(), null) == null) {
                TopicPlan topicPlan = new TopicPlan.Builder()
                        .setName(currentTopic.name())
                        .setAction(PlanAction.REMOVE)
                        .build();

                desiredPlan.addTopicPlans(topicPlan);
            }
        });
    }

    private void planTopicConfigurations(String topicName, TopicDetails topicDetails, TopicPlan.Builder topicPlan) {
        Map<String, TopicConfigPlan> configPlans = new HashMap<>();
        List<ConfigEntry> currentConfigs = kafkaService.describeTopicConfigs(topicName);
        List<ConfigEntry> customConfigs = currentConfigs.stream()
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
                configPlans.put(currentConfig.name(), topicConfigPlan.build());
                topicPlan.setAction(PlanAction.UPDATE);
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
                topicPlan.setAction(PlanAction.UPDATE);
            } else if (!currentConfig.value().equals(value)) {
                topicConfigPlan.setAction(PlanAction.UPDATE);
                configPlans.put(key, topicConfigPlan.build());
                topicPlan.setAction(PlanAction.UPDATE);
            }
        });

        configPlans.forEach((key, plan) -> {
            log.info("[PLAN] Topic {} | [{}] {}", topicName, plan.getAction(), plan.getKey());
            topicPlan.addTopicConfigPlans(plan);
        });
    }

    private void planAcls(DesiredState desiredState, DesiredPlan.Builder desiredPlan) {
        List<AclBinding> currentAcls = kafkaService.getAcls();

        currentAcls.forEach(acl -> {
            Map.Entry<String, AclDetails> detailsEntry = desiredState.getAcls().entrySet().stream()
                    .filter(entry -> entry.getValue().equalsAclBinding(acl))
                    .findFirst().orElse(null);

            AclPlan.Builder aclPlan = new AclPlan.Builder().setAclBinding(acl);

            if (detailsEntry != null) {
                aclPlan.setName(detailsEntry.getKey());
                aclPlan.setAclDetails(detailsEntry.getValue());
                aclPlan.setAction(PlanAction.NO_CHANGE);
            } else {
                aclPlan.setName("Unnamed ACL");
                aclPlan.setAction(PlanAction.REMOVE);
            }
            desiredPlan.addAclPlans(aclPlan.build());
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

    private void applyTopics(DesiredPlan desiredPlan) {
        desiredPlan.getTopicPlans().forEach(topicPlan -> {
            if (topicPlan.getAction() == PlanAction.ADD) {
                LogUtil.printTopicPreApply(topicPlan);
                kafkaService.createTopic(topicPlan.getName(), topicPlan.getTopicDetails().get());
                LogUtil.printPostApply();
            } else if (topicPlan.getAction() == PlanAction.UPDATE) {
                LogUtil.printTopicPreApply(topicPlan);
                topicPlan.getTopicConfigPlans().forEach(topicConfigPlan -> applyTopicConfiguration(topicPlan, topicConfigPlan));
                LogUtil.printPostApply();
            } else if (topicPlan.getAction() == PlanAction.REMOVE) {
                LogUtil.printTopicPreApply(topicPlan);
                kafkaService.deleteTopic(topicPlan.getName());
                LogUtil.printPostApply();
            }
        });
    }

    private void applyTopicConfiguration(TopicPlan topicPlan, TopicConfigPlan topicConfigPlan) {
        Map<ConfigResource, Collection<AlterConfigOp>> configs = new HashMap<>();
        ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topicPlan.getName());
        List<AlterConfigOp> configOps = new ArrayList<>();

        ConfigEntry configEntry = new ConfigEntry(topicConfigPlan.getKey(), topicConfigPlan.getValue().orElse(null));

        // TODO: Make OpType work with append/subtract
        if (topicConfigPlan.getAction() == PlanAction.ADD) {
            configOps.add(new AlterConfigOp(configEntry, AlterConfigOp.OpType.SET));
        } else if (topicConfigPlan.getAction() == PlanAction.UPDATE) {
            configOps.add(new AlterConfigOp(configEntry, AlterConfigOp.OpType.SET));
        } else if (topicConfigPlan.getAction() == PlanAction.REMOVE) {
            configOps.add(new AlterConfigOp(configEntry, AlterConfigOp.OpType.DELETE));
        }

        configs.put(configResource, configOps);

        kafkaService.updateTopicConfig(configs);
    }

    private void applyAcls(DesiredPlan desiredPlan) {
        desiredPlan.getAclPlans().forEach(aclPlan -> {
            if (aclPlan.getAction() == PlanAction.ADD) {
                LogUtil.printAclPreApply(aclPlan);
                kafkaService.createAcl(aclPlan.getAclDetails().get().toAclBinding());
                LogUtil.printPostApply();
            } else if (aclPlan.getAction() == PlanAction.REMOVE) {
                LogUtil.printAclPreApply(aclPlan);
                kafkaService.deleteAcl(aclPlan.getAclBinding().get());
                LogUtil.printPostApply();
            }
        });
    }

    private void initializeLogger(boolean verbose) {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Logger kafka = (Logger) LoggerFactory.getLogger("org.apache.kafka");
        if (verbose) {
            root.setLevel(Level.INFO);
            kafka.setLevel(Level.WARN);
        } else {
            root.setLevel(Level.WARN);
            kafka.setLevel(Level.OFF);
        }
    }
}
