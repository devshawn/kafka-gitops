package com.devshawn.kafka.gitops.service;

import com.devshawn.kafka.gitops.config.KafkaGitopsConfig;
import com.devshawn.kafka.gitops.domain.plan.TopicConfigPlan;
import com.devshawn.kafka.gitops.domain.plan.TopicDetailsPlan;
import com.devshawn.kafka.gitops.exception.KafkaExecutionException;
import com.devshawn.kafka.gitops.exception.ValidationException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewPartitionReassignment;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class KafkaService {
    private static org.slf4j.Logger log = LoggerFactory.getLogger(KafkaService.class);

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

    public void createTopic(String topicName, TopicDetailsPlan topicDetailsPlan, List<TopicConfigPlan> topicConfigPlans) {
        try (final AdminClient adminClient = buildAdminClient()) {
            NewTopic newTopic = new NewTopic(topicName, topicDetailsPlan.getPartitions().get(), topicDetailsPlan.getReplication().get().shortValue());
            newTopic.configs(topicConfigPlans.stream().collect(Collectors.toMap(TopicConfigPlan::getKey, topicConfigPlan -> topicConfigPlan.getValue().get())));
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
    
    public void addTopicPartition(String topicName, int partitions) {
        if (partitions <= 1) {
            throw new IllegalArgumentException("partitions cannot be <= 1");
        }
        try (final AdminClient adminClient = buildAdminClient()) {
            adminClient.createPartitions(Collections.singletonMap(topicName, NewPartitions.increaseTo(partitions))).all().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new KafkaExecutionException("Error thrown when attempting to increate a Kafka topic partition number", ex.getMessage());
        }
    }

    public Collection<Node> describeClusterNodes() {
      try (final AdminClient adminClient = buildAdminClient()) {
          return adminClient.describeCluster().nodes().get();
      } catch (InterruptedException | ExecutionException ex) {
        throw new KafkaExecutionException("Error thrown when attempting to retrieve the Kafka cluster nodes list", ex.getMessage());
      }
    }

    public void updateTopicReplication(Collection<Node> clusterNodes, String topicName, Integer integer) {
        try (final AdminClient adminClient = buildAdminClient()) {
            Map<String, TopicDescription> originalTopicDescription = getTopicDescription(Collections.singleton(topicName));
            Map<TopicPartition, Optional<NewPartitionReassignment>> newReasignement = new HashMap<>();
            originalTopicDescription.values().forEach(topicDescription -> {
                List<TopicPartitionInfo> topicPartitionInfos = topicDescription.partitions();
                topicPartitionInfos.forEach(topicPartitionInfo -> {
                    TopicPartition topicPartition = new TopicPartition(topicName, topicPartitionInfo.partition());
                    List<Integer> replicas = topicPartitionInfo.replicas().stream().map(Node::id).collect(Collectors.toList());
                    // If we need to remove replicas, always remove the last ones
                    while (replicas.size() > integer) {
                        replicas.remove(replicas.size());
                    }
                    // If we need to increase replicas, add partitions randomly in the nodes
                    if(replicas.size() < integer) {
                        List<Integer> nodesIds = clusterNodes.stream().map(Node::id).collect(Collectors.toList());
                        //Find the nodes into which there is no yet partitions
                        List<Integer> possibleNewNodes = nodesIds.stream().collect(Collectors.toList());
                        possibleNewNodes.removeAll(replicas);
                        if(possibleNewNodes.isEmpty() || (possibleNewNodes.size() < integer - replicas.size())) {
                            throw new ValidationException("Error thrown when attempting to update a Kafka topic partition replication: not enougth nodes in the cluster");
                        }
                        Collections.shuffle(possibleNewNodes);
                        while (replicas.size() < integer) {
                            replicas.add(possibleNewNodes.remove(0));
                        }
                    }
                    NewPartitionReassignment newPartitionReassignment = new NewPartitionReassignment(replicas);
                    newReasignement.put(topicPartition, Optional.of(newPartitionReassignment) );
                });
            });
            adminClient.alterPartitionReassignments(newReasignement).all().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new KafkaExecutionException("Error thrown when attempting to update a Kafka topic partition replication", ex.getMessage());
        }
    }
    
    public void updateTopicConfig(Map<ConfigResource, Collection<AlterConfigOp>> configs) {
        try (final AdminClient adminClient = buildAdminClient()) {
            adminClient.incrementalAlterConfigs(configs).all().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new KafkaExecutionException("Error thrown when attempting to update a Kafka topic config", ex.getMessage());
        }
    }

    public Map<String, TopicDescription> getTopics() {
        log.info("Get existing topics...");
        try (final AdminClient adminClient = buildAdminClient()) {
            Set<String> topics = adminClient.listTopics().names().get();
            Map<String, TopicDescription> topicsDescription = getTopicDescription(topics, adminClient);
           log.info("Existing topics retrieved ({})...", topicsDescription.size());
           return topicsDescription;
        } catch (InterruptedException | ExecutionException ex) {
            throw new KafkaExecutionException("Error thrown when attempting to list Kafka topics", ex.getMessage());
        }
    }

    public Map<String, TopicDescription> getTopicDescription(Set<String> topics) {
        try (final AdminClient adminClient = buildAdminClient()) {
            return getTopicDescription(topics, adminClient);
        } catch (Exception ex) {
            throw new KafkaExecutionException("Error thrown when attempting to describe Kafka topics", ex.getMessage());
        }
    }

    private Map<String, TopicDescription> getTopicDescription(Set<String> topics, AdminClient adminClient) throws InterruptedException, ExecutionException {
        return adminClient.describeTopics(topics).all().get();
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
