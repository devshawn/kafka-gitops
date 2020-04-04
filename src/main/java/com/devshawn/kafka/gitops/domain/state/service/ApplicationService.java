package com.devshawn.kafka.gitops.domain.state.service;

import com.devshawn.kafka.gitops.domain.state.AclDetails;
import com.devshawn.kafka.gitops.domain.state.ServiceDetails;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = ApplicationService.Builder.class)
public abstract class ApplicationService extends ServiceDetails {

    public abstract Optional<String> getPrincipal();

    public abstract List<String> getProduces();

    public abstract List<String> getConsumes();

    @Override
    public List<AclDetails.Builder> getAcls(String serviceName) {
        List<AclDetails.Builder> acls = new ArrayList<>();
        getProduces().forEach(topic -> acls.add(generateWriteACL(topic, getPrincipal())));
        getConsumes().forEach(topic -> acls.add(generateReadAcl(topic, getPrincipal())));
        if (!getConsumes().isEmpty()) {
            acls.add(generateConsumerGroupAcl(serviceName, getPrincipal()));
        }
        return acls;
    }

    public static class Builder extends ApplicationService_Builder {

    }
}
