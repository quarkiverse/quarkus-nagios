package io.quarkiverse.nagios.it;

import io.quarkiverse.nagios.health.*;
import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.health.*;

import java.time.Duration;

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
