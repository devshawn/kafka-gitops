package com.devshawn.kafka.gitops.exception

import spock.lang.Specification

class PlanIsUpToDateExceptionSpec extends Specification {

    void 'test PlanIsUpToDateException'() {
        when:
        PlanIsUpToDateException result = new PlanIsUpToDateException()

        then:
        result.message == "The current desired state file matches the actual state of the cluster."
    }
}
