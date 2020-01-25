package com.devshawn.kafka.dsf.domain.state;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
@JsonDeserialize(builder = AclDetails.Builder.class)
public abstract class AclDetails {

    public abstract String getName();

    public abstract String getType();

    public abstract String getPattern();

    public abstract String getPrincipal();

    public abstract String getHost();

    public abstract String getOperation();

    public abstract String getPermission();

    public abstract Optional<Integer> getPrincipalId();

    public boolean equalsAclBinding(AclBinding aclBinding) {
        String principal = getPrincipal();
        if (getPrincipalId().isPresent()) {
            principal = String.format("User:%s", getPrincipalId().get());
        }

        if (aclBinding.pattern().name().equals(getName())
                && aclBinding.pattern().patternType().name().equals(getPattern())
                && aclBinding.pattern().resourceType().name().equals(getType())
                && aclBinding.entry().principal().equals(principal)
                && aclBinding.entry().host().equals(getHost())
                && aclBinding.entry().permissionType().name().equals(getPermission())
                && aclBinding.entry().operation().name().equals(getOperation())) {
            return true;
        }
        return false;
    }

    public AclBinding toAclBinding() {
        String principal = getPrincipal();
        if (getPrincipalId().isPresent()) {
            principal = String.format("User:%s", getPrincipalId().get());
        }
        return new AclBinding(
                new ResourcePattern(ResourceType.valueOf(getType()), getName(), PatternType.valueOf(getPattern())),
                new AccessControlEntry(principal, getHost(), AclOperation.valueOf(getOperation()), AclPermissionType.valueOf(getPermission()))
        );
    }

    public static class Builder extends AclDetails_Builder {
    }
}
