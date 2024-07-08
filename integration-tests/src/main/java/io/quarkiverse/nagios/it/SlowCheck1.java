package io.quarkiverse.nagios.it;

import java.time.Duration;

import org.eclipse.microprofile.health.*;

import io.quarkiverse.nagios.health.*;
import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;

@Liveness
class SlowCheck1 implements AsyncHealthCheck {

    @Override
    public Uni<HealthCheckResponse> call() {
        var response = NagiosCheck.named("My Check")
                .result(NagiosStatus.OK)
                .asResponse();
        return Uni.createFrom().<HealthCheckResponse>item(response)
                .onItem().delayIt().by(Duration.ofMillis(10500));
    }
}
