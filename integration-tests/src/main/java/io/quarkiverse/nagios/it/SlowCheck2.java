package io.quarkiverse.nagios.it;

import java.time.Duration;

import org.eclipse.microprofile.health.*;

import io.quarkiverse.nagios.health.*;
import io.quarkiverse.nagios.unis.*;
import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;

@Liveness
class SlowCheck2 implements AsyncHealthCheck {

    @Override
    public Uni<HealthCheckResponse> call() {
        return DurationHealthCheck.measure(CHECK, this::slowOperation)
                .plug(UniSoftCache.build()
                        .initiallyTimeout("Slow")
                        .cacheForMillis(r -> r.getNagiosStatus() == NagiosStatus.OK ? 60000 : 15000)
                        .waitFor(Duration.ofSeconds(11)))
                .map(NagiosCheckResult::asResponse);
    }

    private void slowOperation() {
        try {
            Thread.sleep(10500);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static final NagiosCheck CHECK = NagiosCheck.named("Slow Check 2")
            .warningIf().above(11000).criticalIf().above(12000).build();
}
