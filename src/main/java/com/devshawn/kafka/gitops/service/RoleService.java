package com.devshawn.kafka.gitops.service;

import com.devshawn.kafka.gitops.domain.state.AclDetails;
import com.devshawn.kafka.gitops.domain.state.ServiceDetails;
import com.devshawn.kafka.gitops.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoleService extends ServiceDetails {

    public List<AclDetails.Builder> getAcls(String role, String principal) {
        switch (role.toLowerCase()) {
            case "reader":
                return getReaderAcls(principal);
            case "writer":
                return getWriterAcls(principal);
            case "operator":
                return getOperatorAcls(principal);
            default:
                throw new ValidationException(String.format("Role '%s' does not exist. Supported roles: 'reader', 'writer', 'operator'.", role));
        }
    }

    private List<AclDetails.Builder> getReaderAcls(String principal) {
        List<AclDetails.Builder> acls = new ArrayList<>();
        acls.add(generateReadAcl("*", Optional.of(principal)));
        acls.add(generateConsumerGroupAcl("*", Optional.of(principal), "READ"));
        return acls;
    }

    private List<AclDetails.Builder> getWriterAcls(String principal) {
        List<AclDetails.Builder> acls = new ArrayList<>();
        acls.add(generateWriteACL("*", Optional.of(principal)));
        return acls;
    }

    private List<AclDetails.Builder> getOperatorAcls(String principal) {
        List<AclDetails.Builder> acls = new ArrayList<>();
        acls.add(getClusterDescribeAcl(principal));
        acls.add(getWildcardTopicAcl(principal, "DESCRIBE"));
        acls.add(getWildcardTopicAcl(principal, "DESCRIBE_CONFIGS"));
        acls.add(generateConsumerGroupAcl("*", Optional.of(principal), "READ"));
        acls.add(generateConsumerGroupAcl("*", Optional.of(principal), "DESCRIBE"));
        return acls;
    }

    private AclDetails.Builder getWildcardTopicAcl(String principal, String operation) {
        return new AclDetails.Builder()
                .setHost("*")
                .setType("TOPIC")
                .setPermission("ALLOW")
                .setPrincipal(principal)
                .setOperation(operation)
                .setPattern("LITERAL")
                .setName("*");
    }

    private AclDetails.Builder getClusterDescribeAcl(String principal) {
        return new AclDetails.Builder()
                .setHost("*")
                .setType("CLUSTER")
                .setPermission("ALLOW")
                .setPrincipal(principal)
                .setOperation("DESCRIBE")
                .setPattern("LITERAL")
                .setName("kafka-cluster");
    }
}
