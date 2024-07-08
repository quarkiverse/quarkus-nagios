package io.quarkiverse.nagios.runtime;

import static io.quarkiverse.nagios.runtime.NagiosStatusReporter.*;

import io.smallrye.health.api.HealthType;
import io.vertx.ext.web.RoutingContext;

public class NagiosStatusTypeHandler extends NagiosStatusGroupHandler {

    @Override
    protected HealthType getType(RoutingContext context) {
        return switch (super.getGroup(context)) {
            case "well" -> HealthType.WELLNESS;
            case "ready" -> HealthType.READINESS;
            case "live" -> HealthType.LIVENESS;
            case "started" -> HealthType.STARTUP;
            default -> null;
        };
    }

    @Override
    protected String getGroup(RoutingContext context) {
        var group = super.getGroup(context);
        return switch (group) {
            case "all" -> HG_ALL;
            case "group" -> HG_GROUP;
            default -> group;
        };
    }
}
