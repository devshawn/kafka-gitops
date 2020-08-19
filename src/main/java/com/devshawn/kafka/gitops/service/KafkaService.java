package com.devshawn.kafka.gitops.service;

import com.devshawn.kafka.gitops.config.KafkaGitopsConfig;
import com.devshawn.kafka.gitops.domain.state.TopicDetails;
import com.devshawn.kafka.gitops.exception.KafkaExecutionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class KafkaService {

    private final KafkaGitopsConfig config;

    public KafkaService(KafkaGitopsConfig config) {
        this.config = config;
    }

    public List<AclBinding> getAcls() {
        try (final AdminClient adminClient = buildAdminClient()) {
            ResourcePatternFilter resourcePatternFilter = new ResourcePatternFilter(ResourceType.ANY, null, PatternType.ANY);
            AccessControlEntryFilter accessFilter = new AccessControlEntryFilter(null, null, AclOperation.ANY, AclPermissionType.ANY);
            AclBindingFilter filter = new AclBindingFilter(resourcePatternFilter, accessFilter);
            return new ArrayList<>(adminClient.describeAcls(filter).values().get());
        } catch (InterruptedException | ExecutionException ex) {
            throw new KafkaExecutionException("Error thrown when attempting to list Kafka ACLs", ex.getMessage());
        }
    }

    public void createAcl(AclBinding aclBinding) {
        try (final AdminClient adminClient = buildAdminClient()) {
            adminClient.createAcls(Collections.singletonList(aclBinding)).all().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new KafkaExecutionException("Error thrown when attempting to create a Kafka ACL", ex.getMessage());
        }
    }

    public void deleteAcl(AclBinding aclBinding) {
        try (final AdminClient adminClient = buildAdminClient()) {
            adminClient.deleteAcls(Collections.singletonList(aclBinding.toFilter())).all().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new KafkaExecutionException("Error thrown when attempting to delete a Kafka ACL", ex.getMessage());
        }
    }

    public void createTopic(String topicName, TopicDetails topicDetails) {
        try (final AdminClient adminClient = buildAdminClient()) {
            NewTopic newTopic = new NewTopic(topicName, topicDetails.getPartitions(), topicDetails.getReplication().get().shortValue());
            newTopic.configs(topicDetails.getConfigs());
            adminClient.createTopics(Collections.singletonList(newTopic)).all().get();
        } catch (InterruptedException | ExecutionException | NoSuchElementException ex) {
            throw new KafkaExecutionException("Error thrown when attempting to create a Kafka topic", ex.getMessage());
        }
    }

    public void deleteTopic(String topicName) {
        try (final AdminClient adminClient = buildAdminClient()) {
            adminClient.deleteTopics(Collections.singletonList(topicName)).all().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new KafkaExecutionException("Error thrown when attempting to delete a Kafka topic", ex.getMessage());
        }
    }

    public void updateTopicConfig(Map<ConfigResource, Collection<AlterConfigOp>> configs) {
        try (final AdminClient adminClient = buildAdminClient()) {
            adminClient.incrementalAlterConfigs(configs).all().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new KafkaExecutionException("Error thrown when attempting to update a Kafka topic config", ex.getMessage());
        }
    }

    public List<TopicListing> getTopics() {
        try (final AdminClient adminClient = buildAdminClient()) {
            Collection<TopicListing> topics = adminClient.listTopics().listings().get();
            return new ArrayList<>(topics);
        } catch (InterruptedException | ExecutionException ex) {
            throw new KafkaExecutionException("Error thrown when attempting to list Kafka topics", ex.getMessage());
        }
    }

    public Map<ConfigResource, Config> describeConfigsForTopics(List<String> topicNames) {
        try (final AdminClient adminClient = buildAdminClient()) {
            List<ConfigResource> resources = topicNames.stream().map(it -> new ConfigResource(ConfigResource.Type.TOPIC, it)).collect(Collectors.toList());
            return adminClient.describeConfigs(resources).all().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new KafkaExecutionException("Error thrown when attempting to describe a Kafka topic configuration", ex.getMessage());
        }
    }

    private AdminClient buildAdminClient() {
        try {
            return KafkaAdminClient.create(config.getConfig());
        } catch (KafkaException ex) {
            throw new KafkaExecutionException("Error thrown when creating Kafka admin client", ex.getCause().getMessage());
        }
    }
}
