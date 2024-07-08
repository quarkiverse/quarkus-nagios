package io.quarkiverse.nagios.unis;

import java.time.OffsetDateTime;
import java.util.function.Supplier;

import org.eclipse.microprofile.health.HealthCheckResponse;

import io.quarkiverse.nagios.health.NagiosCheckResponse;
import io.smallrye.mutiny.Uni;

public class StartupOnlyHealthCheck {

    public static Uni<HealthCheckResponse> of(Uni<? extends HealthCheckResponse> actual) {
        return of(() -> actual);
    }

    public static Uni<HealthCheckResponse> of(Supplier<Uni<? extends HealthCheckResponse>> actual) {
        return new StartupOnlyHealthCheck(actual).get();
    }

    private Uni<HealthCheckResponse> uni;

    protected StartupOnlyHealthCheck(Supplier<Uni<? extends HealthCheckResponse>> actual) {
        uni = Uni.createFrom().deferred(actual).map(response -> {
            if (response.getStatus() == HealthCheckResponse.Status.UP) {
                var ok = NagiosCheckResponse.named(response.getName() + " (Startup)")
                        .withData("passed", OffsetDateTime.now().toString()).up().build();
                uni = Uni.createFrom().item(ok);
            }
            return response;
        });
    }

    public Uni<HealthCheckResponse> get() {
        return Uni.createFrom().deferred(() -> uni);
    }
}
