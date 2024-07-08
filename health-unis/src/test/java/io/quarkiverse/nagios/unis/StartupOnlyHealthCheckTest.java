package io.quarkiverse.nagios.unis;

import io.quarkiverse.nagios.health.*;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class StartupOnlyHealthCheckTest {

    @Test
    void up() {
        var status = new AtomicReference<>(NagiosStatus.CRITICAL);

        var uni = Uni.createFrom().item(() -> NagiosCheckResponse.named("test").status(status.get()).build())
                .plug(StartupOnlyHealthCheck::of);

        assertThat(uni.await().indefinitely().getStatus(), is(HealthCheckResponse.Status.DOWN));

        status.set(NagiosStatus.OK);
        var result = uni.await().indefinitely();
        assertThat(result.getStatus(), is(HealthCheckResponse.Status.UP));
        assertThat(result.getName(), is("test"));

        status.set(NagiosStatus.CRITICAL);
        result = uni.await().indefinitely();
        assertThat(result.getStatus(), is(HealthCheckResponse.Status.UP));
        assertThat(result.getName(), is("test (Startup)"));
    }
}
