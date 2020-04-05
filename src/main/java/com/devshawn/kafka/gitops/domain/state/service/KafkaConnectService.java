package com.devshawn.kafka.gitops.domain.state.service;


import com.devshawn.kafka.gitops.domain.state.AclDetails;
import com.devshawn.kafka.gitops.domain.state.ServiceDetails;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = KafkaConnectService.Builder.class)
public abstract class KafkaConnectService extends ServiceDetails {

    public abstract Optional<String> getPrincipal();

    public abstract List<String> getProduces();

    public abstract Map<String, KafkaConnectorDetails> getConnectors();

    @Override
    public List<AclDetails.Builder> getAcls(String serviceName) {
        List<AclDetails.Builder> acls = new ArrayList<>();
        getProduces().forEach(topic -> acls.add(generateWriteACL(topic, getPrincipal())));
        acls.addAll(getConnectWorkerAcls(serviceName));
        return acls;
    }

    private List<AclDetails.Builder> getConnectWorkerAcls(String serviceName) {
        List<AclDetails.Builder> acls = new ArrayList<>();
        acls.add(generateReadAcl(String.format("connect-configs-%s", serviceName), getPrincipal()));
        acls.add(generateReadAcl(String.format("connect-offsets-%s", serviceName), getPrincipal()));
        acls.add(generateReadAcl(String.format("connect-status-%s", serviceName), getPrincipal()));
        acls.add(generateWriteACL(String.format("connect-configs-%s", serviceName), getPrincipal()));
        acls.add(generateWriteACL(String.format("connect-offsets-%s", serviceName), getPrincipal()));
        acls.add(generateWriteACL(String.format("connect-status-%s", serviceName), getPrincipal()));
        acls.add(generateConsumerGroupAcl(serviceName, getPrincipal(), "READ"));
        getConnectors().forEach((connectorName, connector) -> acls.addAll(connector.getAcls(connectorName, getPrincipal())));
        return acls;
    }

    public static class Builder extends KafkaConnectService_Builder {

    }
}
