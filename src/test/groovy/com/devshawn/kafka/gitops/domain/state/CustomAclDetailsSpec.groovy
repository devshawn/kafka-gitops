package com.devshawn.kafka.gitops.domain.state

import com.devshawn.kafka.gitops.exception.InvalidAclDefinitionException
import org.apache.kafka.common.acl.AclOperation
import org.apache.kafka.common.acl.AclPermissionType
import org.apache.kafka.common.resource.PatternType
import org.apache.kafka.common.resource.ResourceType
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class CustomAclDetailsSpec extends Specification {

    void 'test valid custom acl details - #type #pattern #operation #permission'() {
        setup:
        CustomAclDetails details = new CustomAclDetails.Builder()
                .setName("name")
                .setType(type)
                .setPattern(pattern)
                .setHost("*")
                .setPrincipal("User:test")
                .setOperation(operation)
                .setPermission(permission)
                .build()

        when:
        details.validate()

        then:
        noExceptionThrown()

        where:
        type                        | pattern                     | operation                    | permission
        ResourceType.TOPIC.name()   | PatternType.LITERAL.name()  | AclOperation.READ.name()     | AclPermissionType.ALLOW.name()
        ResourceType.CLUSTER.name() | PatternType.PREFIXED.name() | AclOperation.WRITE.name()    | AclPermissionType.DENY.name()
        ResourceType.GROUP.name()   | PatternType.LITERAL.name()  | AclOperation.CREATE.name()   | AclPermissionType.ALLOW.name()
        ResourceType.GROUP.name()   | PatternType.LITERAL.name()  | AclOperation.DESCRIBE.name() | AclPermissionType.DENY.name()
    }

    void 'test invalid custom acl details - type - #input'() {
        setup:
        CustomAclDetails details = new CustomAclDetails.Builder()
                .setName("name")
                .setType(input)
                .setPattern(PatternType.LITERAL.name())
                .setHost("*")
                .setPrincipal("User:test")
                .setOperation(AclOperation.READ.name())
                .setPermission(AclPermissionType.ALLOW.name())
                .build()

        when:
        details.validate()

        then:
        InvalidAclDefinitionException ex = thrown(InvalidAclDefinitionException)
        ex.getField() == "type"
        ex.getValue() == input
        ex.getAllowedValues() == ["TOPIC", "GROUP", "CLUSTER", "TRANSACTIONAL_ID", "DELEGATION_TOKEN"]

        where:
        input << ["ANY", "UNKNOWN", "not real", "PREFIX", "prefixed", "topic"]
    }

    void 'test invalid custom acl details - pattern - #input'() {
        setup:
        CustomAclDetails details = new CustomAclDetails.Builder()
                .setName("name")
                .setType(ResourceType.TOPIC.name())
                .setPattern(input)
                .setHost("*")
                .setPrincipal("User:test")
                .setOperation(AclOperation.READ.name())
                .setPermission(AclPermissionType.ALLOW.name())
                .build()

        when:
        details.validate()

        then:
        InvalidAclDefinitionException ex = thrown(InvalidAclDefinitionException)
        ex.getField() == "pattern"
        ex.getValue() == input
        ex.getAllowedValues() == ["MATCH", "LITERAL", "PREFIXED"]

        where:
        input << ["ANY", "UNKNOWN", "not real", "PREFIX", "prefixed", "topic"]
    }

    void 'test invalid custom acl details - operation - #input'() {
        setup:
        CustomAclDetails details = new CustomAclDetails.Builder()
                .setName("name")
                .setType(ResourceType.TOPIC.name())
                .setPattern(PatternType.LITERAL.name())
                .setHost("*")
                .setPrincipal("User:test")
                .setOperation(input)
                .setPermission(AclPermissionType.ALLOW.name())
                .build()

        when:
        details.validate()

        then:
        InvalidAclDefinitionException ex = thrown(InvalidAclDefinitionException)
        ex.getField() == "operation"
        ex.getValue() == input
        ex.getAllowedValues() == ["ALL", "READ", "WRITE", "CREATE", "DELETE", "ALTER", "DESCRIBE", "CLUSTER_ACTION", "DESCRIBE_CONFIGS", "ALTER_CONFIGS", "IDEMPOTENT_WRITE"]

        where:
        input << ["ANY", "UNKNOWN", "not real", "PREFIX", "prefixed", "topic"]
    }

    void 'test invalid custom acl details - permission - #input'() {
        setup:
        CustomAclDetails details = new CustomAclDetails.Builder()
                .setName("name")
                .setType(ResourceType.TOPIC.name())
                .setPattern(PatternType.LITERAL.name())
                .setHost("*")
                .setPrincipal("User:test")
                .setOperation(AclOperation.CREATE.name())
                .setPermission(input)
                .build()

        when:
        details.validate()

        then:
        InvalidAclDefinitionException ex = thrown(InvalidAclDefinitionException)
        ex.getField() == "permission"
        ex.getValue() == input
        ex.getAllowedValues() == ["DENY", "ALLOW"]

        where:
        input << ["ANY", "UNKNOWN", "not real", "PREFIX", "prefixed", "topic"]
    }
}
