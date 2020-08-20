package com.devshawn.kafka.gitops.domain.state.service;


import com.devshawn.kafka.gitops.domain.options.GetAclOptions;
import com.devshawn.kafka.gitops.domain.state.AclDetails;
import com.devshawn.kafka.gitops.domain.state.ServiceDetails;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = KafkaConnectService.Builder.class)
public abstract class KafkaConnectService extends ServiceDetails {

    @JsonProperty("group-id")
    public abstract Optional<String> getGroupId();

    public abstract Optional<String> getPrincipal();

    @JsonProperty("storage-topics")
    public abstract Optional<KafkaConnectStorageTopics> getStorageTopics();

    public abstract List<String> getProduces();

    public abstract Map<String, KafkaConnectorDetails> getConnectors();

    @Override
    public List<AclDetails.Builder> getAcls(GetAclOptions options) {
        List<AclDetails.Builder> acls = new ArrayList<>();
        getProduces().forEach(topic -> acls.add(generateWriteACL(topic, getPrincipal())));
        if (options.getDescribeAclEnabled()) {
            getProduces().forEach(topic -> acls.add(generateDescribeAcl(topic, getPrincipal())));
        }
        acls.addAll(getConnectWorkerAcls(options));
        return acls;
    }

    private List<AclDetails.Builder> getConnectWorkerAcls(GetAclOptions options) {
        String groupId = getGroupId().isPresent() ? getGroupId().get() : options.getServiceName();
        String configTopic = getConfigTopic(options.getServiceName());
        String offsetTopic = getOffsetTopic(options.getServiceName());
        String statusTopic = getStatusTopic(options.getServiceName());

        List<AclDetails.Builder> acls = new ArrayList<>();
        acls.add(generateReadAcl(configTopic, getPrincipal()));
        acls.add(generateReadAcl(offsetTopic, getPrincipal()));
        acls.add(generateReadAcl(statusTopic, getPrincipal()));
        acls.add(generateWriteACL(configTopic, getPrincipal()));
        acls.add(generateWriteACL(offsetTopic, getPrincipal()));
        acls.add(generateWriteACL(statusTopic, getPrincipal()));
        acls.add(generateConsumerGroupAcl(groupId, getPrincipal(), "READ"));
        getConnectors().forEach((connectorName, connector) -> acls.addAll(connector.getAcls(connectorName, getPrincipal(), options)));
        return acls;
    }

    private String getConfigTopic(String serviceName) {
        if (getStorageTopics().isPresent() && getStorageTopics().get().getConfig().isPresent()) {
            return getStorageTopics().get().getConfig().get();
        }
        return String.format("connect-configs-%s", serviceName);
    }

    private String getOffsetTopic(String serviceName) {
        if (getStorageTopics().isPresent() && getStorageTopics().get().getOffset().isPresent()) {
            return getStorageTopics().get().getOffset().get();
        }
        return String.format("connect-offsets-%s", serviceName);
    }

    private String getStatusTopic(String serviceName) {
        if (getStorageTopics().isPresent() && getStorageTopics().get().getStatus().isPresent()) {
            return getStorageTopics().get().getStatus().get();
        }
        return String.format("connect-status-%s", serviceName);
    }

    public static class Builder extends KafkaConnectService_Builder {

    }
}
