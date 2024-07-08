package io.quarkiverse.nagios.runtime;

import io.quarkiverse.nagios.health.NagiosCheckResponse;
import io.quarkus.arc.*;
import io.smallrye.health.api.HealthType;
import io.vertx.core.Handler;
import io.vertx.core.http.*;
import io.vertx.ext.web.RoutingContext;

public class NagiosStatusRootHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext context) {
        try (InstanceHandle<NagiosStatusReporter> handle = Arc.container().instance(NagiosStatusReporter.class)) {
            HealthType type = getType(context);
            String group = type != null ? null : getGroup(context);
            NagiosCheckResponse result = handle.get().checkInContext(context, type, group);
            HttpServerResponse resp = context.response();
            resp.headers().set(HttpHeaders.CONTENT_TYPE, "text/plain+nagios; charset=UTF-8");
            resp.end(result.toString(), "UTF-8");
        }
    }

    protected HealthType getType(RoutingContext context) {
        return null;
    }

    protected String getGroup(RoutingContext context) {
        return null;
    }
}
