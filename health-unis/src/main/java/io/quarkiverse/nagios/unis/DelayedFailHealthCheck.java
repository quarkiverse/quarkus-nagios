package io.quarkiverse.nagios.unis;

import java.time.*;
import java.util.*;
import java.util.function.*;

import org.eclipse.microprofile.health.HealthCheckResponse;

import io.quarkiverse.nagios.health.*;
import io.smallrye.mutiny.Uni;

public class DelayedFailHealthCheck {

    static Clock clock = Clock.systemDefaultZone();

    public static Function<Uni<NagiosCheckResponse>, Uni<HealthCheckResponse>> of(Duration warningThreshold,
            Duration criticalThreshold) {
        return actual -> of(actual, warningThreshold, criticalThreshold);
    }

    public static Uni<HealthCheckResponse> of(Uni<NagiosCheckResponse> actual, Duration warningThreshold,
            Duration criticalThreshold) {
        return of(() -> actual, warningThreshold, criticalThreshold);
    }

    public static Uni<HealthCheckResponse> of(Supplier<Uni<NagiosCheckResponse>> actual, Duration warningThreshold,
            Duration criticalThreshold) {
        return new DelayedFailHealthCheck(actual, warningThreshold, criticalThreshold, false).get();
    }

    private final Supplier<Uni<NagiosCheckResponse>> actual;
    private final Duration warningThreshold;
    private final Duration criticalThreshold;
    private OffsetDateTime lastOk;
    private String lastName = "delayed health check";

    protected DelayedFailHealthCheck(Supplier<Uni<NagiosCheckResponse>> actual, Duration warningThreshold,
            Duration criticalThreshold, boolean initialOk) {
        this.actual = actual;
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
        this.lastOk = initialOk ? OffsetDateTime.now(clock) : OffsetDateTime.MIN;
    }

    public Uni<HealthCheckResponse> get() {
        return Uni.createFrom().deferred(this::call);
    }

    private Uni<NagiosCheckResponse> call() {
        return actual.get().map(this::map)
                .onFailure().recoverWithItem(this::recover);
    }

    private NagiosCheckResponse map(NagiosCheckResponse response) {
        switch (response.getNagiosStatus()) {
            case OK:
                lastOk = OffsetDateTime.now(clock);
                lastName = response.getName();
                return response;
            case WARNING:
                if (OffsetDateTime.now(clock).isAfter(lastOk.plus(warningThreshold))) {
                    return response;
                }
                break;
            case CRITICAL:
                if (OffsetDateTime.now(clock).isAfter(lastOk.plus(criticalThreshold))) {
                    return response;
                }
                break;
            default:
                return response;
        }
        return okResponse(response);
    }

    private NagiosCheckResponse okResponse(NagiosCheckResponse response) {
        var checks = new ArrayList<NagiosCheckResult>();
        checks.add(NagiosCheck.named(response.getName()).result(response.getNagiosStatus()));
        checks.addAll(response.getChecks());
        var data = new LinkedHashMap<String, Object>();
        data.put("last ok", lastOk.toString());
        data.putAll(response.getDataMap());
        return new NagiosCheckResponse(response.getName(), NagiosStatus.OK, checks, response.getPerformanceValues(), data);
    }

    private NagiosCheckResponse recover(Throwable t) {
        var status = OffsetDateTime.now(clock).isAfter(lastOk.plus(criticalThreshold)) ? NagiosStatus.CRITICAL
                : NagiosStatus.WARNING;
        // Log.infof(t, "Recovered delayed health check %s as %s", lastName, status);
        return NagiosCheckResponse.named(lastName).status(status)
                .withData("last ok", lastOk.toString())
                .withData("error", t.toString())
                .build();
    }
}
