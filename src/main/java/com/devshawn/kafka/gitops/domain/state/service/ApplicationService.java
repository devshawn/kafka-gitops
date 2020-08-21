package com.devshawn.kafka.gitops.domain.state.service;

import com.devshawn.kafka.gitops.domain.options.GetAclOptions;
import com.devshawn.kafka.gitops.domain.state.AclDetails;
import com.devshawn.kafka.gitops.domain.state.ServiceDetails;
import com.devshawn.kafka.gitops.util.HelperUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = ApplicationService.Builder.class)
public abstract class ApplicationService extends ServiceDetails {

    public abstract Optional<String> getPrincipal();

    @JsonProperty("group-id")
    public abstract Optional<String> getGroupId();

    public abstract List<String> getProduces();

    public abstract List<String> getConsumes();

    @Override
    public List<AclDetails.Builder> getAcls(GetAclOptions options) {
        List<AclDetails.Builder> acls = new ArrayList<>();
        getProduces().forEach(topic -> acls.add(generateWriteACL(topic, getPrincipal())));
        getConsumes().forEach(topic -> acls.add(generateReadAcl(topic, getPrincipal())));

        if (options.getDescribeAclEnabled()) {
            List<String> allTopics = HelperUtil.uniqueCombine(getConsumes(), getProduces());
            allTopics.forEach(topic -> acls.add(generateDescribeAcl(topic, getPrincipal())));
        }

        if (!getConsumes().isEmpty()) {
            String groupId = getGroupId().isPresent() ? getGroupId().get() : options.getServiceName();
            acls.add(generateConsumerGroupAcl(groupId, getPrincipal(), "READ"));
        }
        return acls;
    }

    public static class Builder extends ApplicationService_Builder {

    }
}
