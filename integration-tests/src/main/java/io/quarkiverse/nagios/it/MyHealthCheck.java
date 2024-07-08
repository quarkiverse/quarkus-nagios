package io.quarkiverse.nagios.it;

import org.eclipse.microprofile.health.HealthCheckResponse;

import io.quarkiverse.nagios.health.*;
import io.smallrye.health.api.*;
import io.smallrye.mutiny.Uni;

@Wellness
class MyHealthCheck implements AsyncHealthCheck {

    @Override
    public Uni<HealthCheckResponse> call() {
        var response = NagiosCheck.named("My Check")
                .result(NagiosStatus.WARNING)
                .asResponse();
        return Uni.createFrom().item(response);
    }
}
