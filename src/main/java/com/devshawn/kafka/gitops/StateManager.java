package com.devshawn.kafka.gitops;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.devshawn.kafka.gitops.config.KafkaGitopsConfigLoader;
import com.devshawn.kafka.gitops.config.ManagerConfig;
import com.devshawn.kafka.gitops.domain.confluent.ServiceAccount;
import com.devshawn.kafka.gitops.domain.plan.DesiredPlan;
import com.devshawn.kafka.gitops.domain.state.AclDetails;
import com.devshawn.kafka.gitops.domain.state.CustomAclDetails;
import com.devshawn.kafka.gitops.domain.state.DesiredState;
import com.devshawn.kafka.gitops.domain.state.DesiredStateFile;
import com.devshawn.kafka.gitops.domain.state.service.KafkaStreamsService;
import com.devshawn.kafka.gitops.exception.ConfluentCloudException;
import com.devshawn.kafka.gitops.exception.MissingConfigurationException;
import com.devshawn.kafka.gitops.exception.ServiceAccountNotFoundException;
import com.devshawn.kafka.gitops.manager.ApplyManager;
import com.devshawn.kafka.gitops.manager.PlanManager;
import com.devshawn.kafka.gitops.service.ConfluentCloudService;
import com.devshawn.kafka.gitops.service.KafkaService;
import com.devshawn.kafka.gitops.service.ParserService;
import com.devshawn.kafka.gitops.util.LogUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class StateManager {

    private static org.slf4j.Logger log = LoggerFactory.getLogger(StateManager.class);

    private final ManagerConfig managerConfig;
    private final ObjectMapper objectMapper;
    private final ParserService parserService;
    private final KafkaService kafkaService;
    private final ConfluentCloudService confluentCloudService;

    private PlanManager planManager;
    private ApplyManager applyManager;

    public StateManager(ManagerConfig managerConfig, ParserService parserService) {
        initializeLogger(managerConfig.isVerboseRequested());
        this.managerConfig = managerConfig;
        this.objectMapper = initializeObjectMapper();
        this.kafkaService = new KafkaService(KafkaGitopsConfigLoader.load());
        this.parserService = parserService;
        this.confluentCloudService = new ConfluentCloudService(objectMapper);
        this.planManager = new PlanManager(managerConfig, kafkaService, objectMapper);
        this.applyManager = new ApplyManager(managerConfig, kafkaService);
    }

    public void validate() {
        parserService.parseStateFile();
    }

    public DesiredPlan plan() {
        DesiredPlan desiredPlan = generatePlan();
        planManager.validatePlanHasChanges(desiredPlan);
        return desiredPlan;
    }

    private DesiredPlan generatePlan() {
        DesiredState desiredState = getDesiredState();
        DesiredPlan.Builder desiredPlan = new DesiredPlan.Builder();
        planManager.planAcls(desiredState, desiredPlan);
        planManager.planTopics(desiredState, desiredPlan);
        return desiredPlan.build();
    }

    public DesiredPlan apply() {
        DesiredPlan desiredPlan = planManager.readPlanFromFile();
        if (desiredPlan == null) {
            desiredPlan = generatePlan();
        }

        planManager.validatePlanHasChanges(desiredPlan);

        applyManager.applyTopics(desiredPlan);
        applyManager.applyAcls(desiredPlan);

        return desiredPlan;
    }

    public void createServiceAccounts() {
        DesiredStateFile desiredStateFile = parserService.parseStateFile();
        List<ServiceAccount> serviceAccounts = confluentCloudService.getServiceAccounts();
        AtomicInteger count = new AtomicInteger();
        if (isConfluentCloudEnabled(desiredStateFile)) {
            desiredStateFile.getServices().forEach((name, service) -> {
                if (serviceAccounts.stream().noneMatch(it -> it.getName().equals(name))) {
                    confluentCloudService.createServiceAccount(name);
                    LogUtil.printSimpleSuccess(String.format("Successfully created service account: %s", name));
                    count.getAndIncrement();
                }
            });
        } else {
            throw new ConfluentCloudException("Confluent Cloud must be enabled in the state file to use this command.");
        }

        if (count.get() == 0) {
            LogUtil.printSimpleSuccess("No service accounts were created as there are no new service accounts.");
        }
    }

    private DesiredState getDesiredState() {
        DesiredStateFile desiredStateFile = parserService.parseStateFile();
        DesiredState.Builder desiredState = new DesiredState.Builder()
                .addAllPrefixedTopicsToIgnore(getPrefixedTopicsToIgnore(desiredStateFile))
                .putAllTopics(desiredStateFile.getTopics());

        if (isConfluentCloudEnabled(desiredStateFile)) {
            generateConfluentCloudServiceAcls(desiredState, desiredStateFile);
        } else {
            generateServiceAcls(desiredState, desiredStateFile);
        }

        return desiredState.build();
    }

    private void generateConfluentCloudServiceAcls(DesiredState.Builder desiredState, DesiredStateFile desiredStateFile) {
        List<ServiceAccount> serviceAccounts = confluentCloudService.getServiceAccounts();
        desiredStateFile.getServices().forEach((name, service) -> {
            AtomicReference<Integer> index = new AtomicReference<>(0);

            Optional<ServiceAccount> serviceAccount = serviceAccounts.stream().filter(it -> it.getName().equals(name)).findFirst();
            String serviceAccountId = serviceAccount.orElseThrow(() -> new ServiceAccountNotFoundException(name)).getId();

            service.getAcls(name).forEach(aclDetails -> {
                aclDetails.setPrincipal(String.format("User:%s", serviceAccountId));
                desiredState.putAcls(String.format("%s-%s", name, index.getAndSet(index.get() + 1)), aclDetails.build());
            });

            if (desiredStateFile.getCustomServiceAcls().containsKey(name)) {
                Map<String, CustomAclDetails> customAcls = desiredStateFile.getCustomServiceAcls().get(name);
                customAcls.forEach((aclName, customAcl) -> {
                    AclDetails.Builder aclDetails = AclDetails.fromCustomAclDetails(customAcl);
                    aclDetails.setPrincipal(String.format("User:%s", serviceAccountId));
                    desiredState.putAcls(String.format("%s-%s", name, index.getAndSet(index.get() + 1)), aclDetails.build());
                });
            }
        });
    }

    private void generateServiceAcls(DesiredState.Builder desiredState, DesiredStateFile desiredStateFile) {
        desiredStateFile.getServices().forEach((name, service) -> {
            AtomicReference<Integer> index = new AtomicReference<>(0);
            service.getAcls(name).forEach(aclDetails -> {
                desiredState.putAcls(String.format("%s-%s", name, index.getAndSet(index.get() + 1)), aclDetails.build());
            });

            if (desiredStateFile.getCustomServiceAcls().containsKey(name)) {
                Map<String, CustomAclDetails> customAcls = desiredStateFile.getCustomServiceAcls().get(name);
                customAcls.forEach((aclName, customAcl) -> {
                    AclDetails.Builder aclDetails = AclDetails.fromCustomAclDetails(customAcl);
                    aclDetails.setPrincipal(customAcl.getPrincipal().orElseThrow(() ->
                            new MissingConfigurationException(String.format("Missing principal for custom ACL %s", aclName))));
                    desiredState.putAcls(String.format("%s-%s", name, index.getAndSet(index.get() + 1)), aclDetails.build());
                });
            }
        });
    }

    private List<String> getPrefixedTopicsToIgnore(DesiredStateFile desiredStateFile) {
        List<String> topics = new ArrayList<>();
        try {
            topics.addAll(desiredStateFile.getSettings().get().getTopics().get().getBlacklist().get().getPrefixed());
        } catch (NoSuchElementException ex) {
            // Do nothing, no blacklist exists
        }
        desiredStateFile.getServices().forEach((name, service) -> {
            if (service instanceof KafkaStreamsService) {
                topics.add(name);
            }
        });
        return topics;
    }

    private boolean isConfluentCloudEnabled(DesiredStateFile desiredStateFile) {
        if (desiredStateFile.getSettings().isPresent() && desiredStateFile.getSettings().get().getCcloud().isPresent()) {
            return desiredStateFile.getSettings().get().getCcloud().get().isEnabled();
        }
        return false;
    }

    private ObjectMapper initializeObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new Jdk8Module());
        return objectMapper;
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
