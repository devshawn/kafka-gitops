package com.devshawn.kafka.gitops.domain.state.service;

import com.devshawn.kafka.gitops.domain.options.GetAclOptions;
import com.devshawn.kafka.gitops.domain.state.AbstractService;
import com.devshawn.kafka.gitops.domain.state.AclDetails;
import com.devshawn.kafka.gitops.util.HelperUtil;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = KafkaConnectorDetails.Builder.class)
public abstract class KafkaConnectorDetails extends AbstractService {

    public abstract List<String> getProduces();

    public abstract List<String> getConsumes();

    public List<AclDetails.Builder> getAcls(String connectorName, Optional<String> principal, GetAclOptions options) {
        List<AclDetails.Builder> acls = new ArrayList<>();
        getProduces().forEach(topic -> acls.add(generateWriteACL(topic, principal)));
        getConsumes().forEach(topic -> acls.add(generateReadAcl(topic, principal)));

        if (options.getDescribeAclEnabled()) {
            List<String> allTopics = HelperUtil.uniqueCombine(getConsumes(), getProduces());
            allTopics.forEach(topic -> acls.add(generateDescribeAcl(topic, principal)));
        }

        if (!getConsumes().isEmpty()) {
            acls.add(generateConsumerGroupAcl(String.format("connect-%s", connectorName), principal, "READ"));
        }
        return acls;
    }

    public static class Builder extends KafkaConnectorDetails_Builder {

    }
}
