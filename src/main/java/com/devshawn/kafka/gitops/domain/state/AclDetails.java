package com.devshawn.kafka.gitops.domain.state;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.inferred.freebuilder.FreeBuilder;

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

    public static AclDetails fromAclBinding(AclBinding aclBinding) {
        AclDetails.Builder aclDetails = new AclDetails.Builder()
                .setName(aclBinding.pattern().name())
                .setType(aclBinding.pattern().resourceType().name())
                .setPattern(aclBinding.pattern().patternType().name())
                .setPrincipal(aclBinding.entry().principal())
                .setHost(aclBinding.entry().host())
                .setOperation(aclBinding.entry().operation().name())
                .setPermission(aclBinding.entry().permissionType().name());
        return aclDetails.build();
    }

    public static AclDetails.Builder fromCustomAclDetails(CustomAclDetails customAclDetails) {
        return new AclDetails.Builder()
                .setName(customAclDetails.getName())
                .setType(customAclDetails.getType())
                .setPattern(customAclDetails.getPattern())
                .setHost(customAclDetails.getHost())
                .setOperation(customAclDetails.getOperation())
                .setPermission(customAclDetails.getPermission());
    }

    public boolean equalsAclBinding(AclBinding aclBinding) {
        if (aclBinding.pattern().name().equals(getName())
                && aclBinding.pattern().patternType().name().equals(getPattern())
                && aclBinding.pattern().resourceType().name().equals(getType())
                && aclBinding.entry().principal().equals(getPrincipal())
                && aclBinding.entry().host().equals(getHost())
                && aclBinding.entry().permissionType().name().equals(getPermission())
                && aclBinding.entry().operation().name().equals(getOperation())) {
            return true;
        }
        return false;
    }

    public AclBinding toAclBinding() {
        return new AclBinding(
                new ResourcePattern(ResourceType.valueOf(getType()), getName(), PatternType.valueOf(getPattern())),
                new AccessControlEntry(getPrincipal(), getHost(), AclOperation.valueOf(getOperation()), AclPermissionType.valueOf(getPermission()))
        );
    }

    public static class Builder extends AclDetails_Builder {
        public Builder() {
            setPermission(AclPermissionType.ALLOW.name());
            setHost("*");
        }
    }
}
