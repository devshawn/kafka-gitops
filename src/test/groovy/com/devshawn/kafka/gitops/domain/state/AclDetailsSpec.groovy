package com.devshawn.kafka.gitops.domain.state

import spock.lang.Specification
import spock.lang.Unroll

class AclDetailsSpec extends Specification {

    @Unroll
    void 'test fromCustomAclDetails - #name'() {
        setup:
        CustomAclDetails customAclDetails = new CustomAclDetails.Builder()
                .setName(name)
                .setType(type)
                .setPattern(pattern)
                .setHost(host)
                .setOperation(operation)
                .setPermission(permission)
                .setPrincipal(Optional.ofNullable(principal))
                .build()

        when:
        AclDetails.Builder result = AclDetails.fromCustomAclDetails(customAclDetails)

        then:
        result
        result.name == name
        result.type == type
        result.pattern == pattern
        result.host == host
        result.operation == operation
        result.permission == permission

        when:
        result.principal == null

        then:
        thrown(IllegalStateException)

        where:
        name           | type    | pattern    | host        | operation  | permission | principal
        "test"         | "TOPIC" | "LITERAL"  | "*"         | "DESCRIBE" | "ALLOW"    | null
        "topic-one"    | "TOPIC" | "LITERAL"  | "*"         | "WRITE"    | "DENY"     | "super.admin"
        "another-name" | "GROUP" | "PREFIXED" | "localhost" | "READ"     | "ALLOW"    | "User:12345"
    }
}
