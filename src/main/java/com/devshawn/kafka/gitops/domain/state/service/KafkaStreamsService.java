package com.devshawn.kafka.gitops.domain.state.service;

import com.devshawn.kafka.gitops.domain.state.ServiceDetails;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.List;

@FreeBuilder
@JsonDeserialize(builder = KafkaStreamsService.Builder.class)
public abstract class KafkaStreamsService extends ServiceDetails {

    public abstract List<String> getProduces();

    public abstract List<String> getConsumes();

    public static class Builder extends KafkaStreamsService_Builder {
    }
}
