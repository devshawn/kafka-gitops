package com.devshawn.kafka.gitops.domain.state.service;

import com.devshawn.kafka.gitops.domain.state.AclDetails;
import com.devshawn.kafka.gitops.domain.state.ServiceDetails;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = KafkaStreamsService.Builder.class)
public abstract class KafkaStreamsService extends ServiceDetails {

    public abstract Optional<String> getPrincipal();

    @JsonProperty("application-id")
    public abstract Optional<String> getApplicationId();

    public abstract List<String> getProduces();

    public abstract List<String> getConsumes();

    @Override
    public List<AclDetails.Builder> getAcls(String serviceName) {
        List<AclDetails.Builder> acls = new ArrayList<>();
        getProduces().forEach(topic -> acls.add(generateWriteACL(topic, getPrincipal())));
        getConsumes().forEach(topic -> acls.add(generateReadAcl(topic, getPrincipal())));
        acls.addAll(getInternalAcls(serviceName));
        return acls;
    }

    private List<AclDetails.Builder> getInternalAcls(String serviceName) {
        String applicationId = getApplicationId().isPresent() ? getApplicationId().get() : serviceName;
        List<AclDetails.Builder> acls = new ArrayList<>();
        acls.add(generatePrefixedTopicACL(applicationId, getPrincipal(), "READ"));
        acls.add(generatePrefixedTopicACL(applicationId, getPrincipal(), "WRITE"));
        acls.add(generatePrefixedTopicACL(applicationId, getPrincipal(), "DESCRIBE"));
        acls.add(generatePrefixedTopicACL(applicationId, getPrincipal(), "DELETE"));
        acls.add(generatePrefixedTopicACL(applicationId, getPrincipal(), "CREATE"));
        acls.add(generatePrefixedTopicACL(applicationId, getPrincipal(), "ALTER"));
        acls.add(generatePrefixedTopicACL(applicationId, getPrincipal(), "ALTER_CONFIGS"));
        acls.add(generatePrefixedTopicACL(applicationId, getPrincipal(), "DESCRIBE_CONFIGS"));
        acls.add(generateConsumerGroupAcl(applicationId, getPrincipal(), "READ"));
        acls.add(generateConsumerGroupAcl(applicationId, getPrincipal(), "DESCRIBE"));
        return acls;
    }

    public static class Builder extends KafkaStreamsService_Builder {
    }
}
