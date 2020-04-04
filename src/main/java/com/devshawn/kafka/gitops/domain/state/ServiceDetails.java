package com.devshawn.kafka.gitops.domain.state;

import com.devshawn.kafka.gitops.domain.state.service.ApplicationService;
import com.devshawn.kafka.gitops.domain.state.service.KafkaConnectService;
import com.devshawn.kafka.gitops.domain.state.service.KafkaStreamsService;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

@JsonSubTypes({
        @JsonSubTypes.Type(value = ApplicationService.class, name = "application"),
        @JsonSubTypes.Type(value = KafkaConnectService.class, name = "kafka-connect"),
        @JsonSubTypes.Type(value = KafkaStreamsService.class, name = "kafka-streams")
})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class ServiceDetails extends AbstractService {

    public String type;

    public List<AclDetails.Builder> getAcls(String serviceName) {
        throw new UnsupportedOperationException("Method getAcls is not implemented.");
    }
}
