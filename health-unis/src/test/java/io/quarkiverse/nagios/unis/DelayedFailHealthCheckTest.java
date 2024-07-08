package io.quarkiverse.nagios.unis;

import io.quarkiverse.nagios.health.*;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.*;

import java.time.*;
import java.util.concurrent.atomic.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class DelayedFailHealthCheckTest {

    @AfterEach
    void tearDown() {
        DelayedFailHealthCheck.clock = Clock.systemDefaultZone();
    }

    @Test
    void okResponse() {
        var status = new AtomicReference<>(NagiosStatus.OK);
        setClock(0);

        var uni = Uni.createFrom().item(() -> NagiosCheckResponse.named("test").status(status.get()).build())
                .plug(DelayedFailHealthCheck.of(Duration.ofSeconds(30), Duration.ofSeconds(10)))
                .map(r -> (NagiosCheckResponse) r);
        assertThat(uni.await().indefinitely().getNagiosStatus(), is(NagiosStatus.OK));

        status.set(NagiosStatus.CRITICAL);
        setClock(5);
        assertThat(uni.await().indefinitely().getNagiosStatus(), is(NagiosStatus.OK));
        setClock(15);
        assertThat(uni.await().indefinitely().getNagiosStatus(), is(NagiosStatus.CRITICAL));

        status.set(NagiosStatus.WARNING);
        assertThat(uni.await().indefinitely().getNagiosStatus(), is(NagiosStatus.OK));
        setClock(35);
        assertThat(uni.await().indefinitely().getNagiosStatus(), is(NagiosStatus.WARNING));
    }

    @Test
    void recover() {
        var status = new AtomicBoolean(true);
        setClock(0);

        var uni = Uni.createFrom().item(() -> {
            if (!status.get())
                throw new IllegalStateException("not ok");
            return NagiosCheckResponse.named("test").status(NagiosStatus.OK).build();
        })
                .plug(DelayedFailHealthCheck.of(Duration.ofSeconds(30), Duration.ofSeconds(10)))
                .map(r -> (NagiosCheckResponse) r);
        assertThat(uni.await().indefinitely().getNagiosStatus(), is(NagiosStatus.OK));

        status.set(false);
        setClock(5);
        assertThat(uni.await().indefinitely().getNagiosStatus(), is(NagiosStatus.WARNING));
        setClock(15);
        assertThat(uni.await().indefinitely().getNagiosStatus(), is(NagiosStatus.CRITICAL));

        status.set(true);
        assertThat(uni.await().indefinitely().getNagiosStatus(), is(NagiosStatus.OK));
    }

    private void setClock(int seconds) {
        var now = OffsetDateTime.of(2024, 4, 11, 12, 0, 0, 0, ZoneOffset.UTC);
        DelayedFailHealthCheck.clock = Clock.fixed(now.plusSeconds(seconds).toInstant(), ZoneOffset.UTC);
    }
}
