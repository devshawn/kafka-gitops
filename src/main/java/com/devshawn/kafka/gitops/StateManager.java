package com.devshawn.kafka.gitops;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.devshawn.kafka.gitops.config.KafkaGitopsConfigLoader;
import com.devshawn.kafka.gitops.config.ManagerConfig;
import com.devshawn.kafka.gitops.domain.confluent.ServiceAccount;
import com.devshawn.kafka.gitops.domain.plan.DesiredPlan;
import com.devshawn.kafka.gitops.domain.state.DesiredState;
import com.devshawn.kafka.gitops.domain.state.DesiredStateFile;
import com.devshawn.kafka.gitops.exception.ServiceAccountNotFoundException;
import com.devshawn.kafka.gitops.manager.ApplyManager;
import com.devshawn.kafka.gitops.manager.PlanManager;
import com.devshawn.kafka.gitops.service.ConfluentCloudService;
import com.devshawn.kafka.gitops.service.KafkaService;
import com.devshawn.kafka.gitops.service.ParserService;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
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

    public StateManager(ManagerConfig managerConfig) {
        initializeLogger(managerConfig.isVerboseRequested());
        this.managerConfig = managerConfig;
        this.objectMapper = initializeObjectMapper();
        this.kafkaService = new KafkaService(KafkaGitopsConfigLoader.load());
        this.parserService = new ParserService(managerConfig.getStateFile());
        this.confluentCloudService = new ConfluentCloudService(objectMapper);
        this.planManager = new PlanManager(managerConfig, kafkaService, objectMapper);
        this.applyManager = new ApplyManager(managerConfig, kafkaService);
    }

    public DesiredPlan plan() {
        DesiredPlan desiredPlan = generatePlan();
        planManager.validatePlanHasChanges(desiredPlan);
        return desiredPlan;
    }

    public DesiredPlan generatePlan() {
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

    private DesiredState getDesiredState() {
        DesiredStateFile desiredStateFile = parserService.parseStateFile();
        DesiredState.Builder desiredState = new DesiredState.Builder()
                .setSettings(desiredStateFile.getSettings())
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
            service.getAcls(name).forEach(aclDetails -> {
                Optional<ServiceAccount> serviceAccount = serviceAccounts.stream().filter(it -> it.getName().equals(name)).findFirst();
                String serviceAccountId = serviceAccount.orElseThrow(() -> new ServiceAccountNotFoundException(name)).getId();
                aclDetails.setPrincipal(String.format("User:%s", serviceAccountId));
                desiredState.putAcls(String.format("%s-%s", name, index.getAndSet(index.get() + 1)), aclDetails.build());
            });
        });
    }

    private void generateServiceAcls(DesiredState.Builder desiredState, DesiredStateFile desiredStateFile) {
        desiredStateFile.getServices().forEach((name, service) -> {
            AtomicReference<Integer> index = new AtomicReference<>(0);
            service.getAcls(name).forEach(aclDetails -> {
                desiredState.putAcls(String.format("%s-%s", name, index.getAndSet(index.get() + 1)), aclDetails.build());
            });
        });
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
