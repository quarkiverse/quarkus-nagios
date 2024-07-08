package io.quarkiverse.nagios.health;

import org.eclipse.microprofile.health.HealthCheckResponse;

public enum NagiosStatus {

    OK,
    WARNING,
    UNKNOWN,
    CRITICAL;

    public NagiosStatus and(NagiosStatus other) {
        if (other != this && other != null && other.ordinal() > ordinal()) {
            return other;
        }
        return this;
    }

    public boolean isUp() {
        return ordinal() <= WARNING.ordinal();
    }

    public HealthCheckResponse.Status toHealth() {
        return isUp() ? HealthCheckResponse.Status.UP : HealthCheckResponse.Status.DOWN;
    }

    public static NagiosStatus ofHealth(HealthCheckResponse.Status status) {
        return status == HealthCheckResponse.Status.UP ? OK : CRITICAL;
    }
}
