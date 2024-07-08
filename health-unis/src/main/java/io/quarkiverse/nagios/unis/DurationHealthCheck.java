package io.quarkiverse.nagios.unis;

import java.util.function.Supplier;

import io.quarkiverse.nagios.health.*;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class DurationHealthCheck {

    private DurationHealthCheck() {
    }

    public static Uni<NagiosCheckResult> measure(NagiosCheck check, Runnable action) {
        var uni = Uni.createFrom().item(() -> {
            action.run();
            return null;
        });
        return measure(check, () -> uni);
    }

    public static Uni<NagiosCheckResult> measure(NagiosCheck check, Supplier<Uni<?>> action) {
        var uni = Uni.createFrom().voidItem()
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .flatMap(n -> {
                    var start = System.currentTimeMillis();
                    return action.get().map(v -> System.currentTimeMillis() - start);
                });
        return of(check, () -> uni);
    }

    public static Uni<NagiosCheckResult> of(NagiosCheck check, Supplier<Uni<Long>> duration) {
        return Uni.createFrom().deferred(duration::get).map(check::result);
    }
}
