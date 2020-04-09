package com.devshawn.kafka.gitops.domain.state

import spock.lang.Specification

class ServiceDetailsSpec extends Specification {

    void 'test default getAcls'() {
        setup:
        ServiceDetails serviceDetails = new ServiceDetails() {}

        when:
        serviceDetails.getAcls("serviceName")

        then:
        UnsupportedOperationException ex = thrown(UnsupportedOperationException)
        ex.message == "Method getAcls is not implemented."
    }
}
