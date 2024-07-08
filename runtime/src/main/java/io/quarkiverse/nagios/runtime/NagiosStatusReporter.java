package io.quarkiverse.nagios.runtime;

import io.quarkiverse.nagios.health.*;
import io.quarkus.arc.*;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.health.*;
import io.smallrye.health.api.*;
import io.smallrye.health.registry.*;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.*;
import jakarta.inject.Inject;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.*;

@ApplicationScoped
public class NagiosStatusReporter {

    public static final String HG_GROUP = "/group/";
    public static final String HG_WELL_OR_GROUP = "/well/or/group/";
    public static final String HG_ALL = "/all/";

    @Inject
    @Any
    Instance<HealthCheck> checks;

    @Inject
    @Any
    Instance<AsyncHealthCheck> asyncChecks;

    @Inject
    AsyncHealthCheckFactory asyncHealthCheckFactory;

    @Inject
    Instance<CurrentIdentityAssociation> identityAssociation;

    @ConfigProperty(name = "quarkus.nagios.default-group", defaultValue = HG_WELL_OR_GROUP)
    String defaultGroup;

    @ConfigProperty(name = "quarkus.nagios.timeout", defaultValue = "11s")
    Duration timeout;

    boolean initialized = false;

    @ActivateRequestContext
    NagiosCheckResponse checkInContext(RoutingContext context, HealthType type, String group) {
        User user = context.user();
        if (user instanceof QuarkusHttpUser quarkusUser && identityAssociation.isResolvable()) {
            identityAssociation.get().setIdentity(quarkusUser.getSecurityIdentity());
        }
        return check(type, group);
    }

    public NagiosCheckResponse check(HealthType type, String group) {
        init();
        if (type == null && group == null)
            group = defaultGroup;
        return Uni.combine().all()
                .unis(getHealthResponses(type, group))
                .with(HealthCheckResponse.class,
                        responses -> NagiosCheckResponse.named("Report").withChecks(responses).build())
                .await().atMost(timeout.plusSeconds(2));
    }

    private void init() {
        if (initialized)
            return;
        try (InstanceHandle<SmallRyeHealthReporter> srHealthHandle = Arc.container().instance(SmallRyeHealthReporter.class)) {
            srHealthHandle.get().getHealth();
        }
        initialized = true;
    }

    private List<Uni<HealthCheckResponse>> getHealthResponses(HealthType type, String group) {
        List<Uni<HealthCheckResponse>> result = new ArrayList<>();
        Predicate<Instance.Handle<?>> includes = includesFilter(type, group);
        addHealthResponses(checks.handlesStream(), includes, check -> result.add(callSync(check)));
        addHealthResponses(asyncChecks.handlesStream(), includes, check -> result.add(callAsync(check)));
        addHealthResponses(getRegistries(type, group), result);
        if (result.isEmpty()) {
            result.add(Uni.createFrom().item(NagiosCheckResponse.named("not found").status(NagiosStatus.UNKNOWN).build()));
        }
        return result;
    }

    private Uni<HealthCheckResponse> callSync(HealthCheck check) {
        return withTimeout(check.getClass().getName(), asyncHealthCheckFactory.callSync(check));
    }

    private Uni<HealthCheckResponse> callAsync(AsyncHealthCheck check) {
        return withTimeout(check.getClass().getName(), asyncHealthCheckFactory.callAsync(check));
    }

    private Uni<HealthCheckResponse> withTimeout(String name, Uni<HealthCheckResponse> uni) {
        return uni.ifNoItem().after(timeout)
                .recoverWithItem(() -> HealthCheckResponse.down(name + " Timeout"));
    }

    private Predicate<Instance.Handle<?>> includesFilter(HealthType type, String group) {
        return typeOrGroupFilter(type, group)
                .and(h -> qualifiers(h).noneMatch(NagiosExcluded.class::isInstance));
    }

    private Predicate<Instance.Handle<?>> typeOrGroupFilter(HealthType type, String group) {
        if (type != null)
            return typeFilter(type);
        return groupFilter(group);
    }

    private Predicate<Instance.Handle<?>> typeFilter(HealthType type) {
        return switch (type) {
            case WELLNESS -> handle -> qualifiers(handle).anyMatch(Wellness.class::isInstance);
            case READINESS -> handle -> qualifiers(handle).anyMatch(Readiness.class::isInstance);
            case LIVENESS -> handle -> qualifiers(handle).anyMatch(Liveness.class::isInstance);
            case STARTUP -> handle -> qualifiers(handle).anyMatch(Startup.class::isInstance);
            default -> throw new IllegalArgumentException("" + type);
        };
    }

    private Predicate<Instance.Handle<?>> groupFilter(String group) {
        return switch (group) {
            case HG_WELL_OR_GROUP -> typeFilter(HealthType.WELLNESS).or(groupFilter(HG_GROUP));
            case HG_ALL -> h -> true;
            case HG_GROUP -> h -> groups(h).anyMatch(hg -> true);
            default -> h -> groups(h).anyMatch(hg -> group.equals(hg.value()));
        };
    }

    private Stream<Annotation> qualifiers(Instance.Handle<?> handle) {
        return handle.getBean().getQualifiers().stream();
    }

    private Stream<HealthGroup> groups(Instance.Handle<?> handle) {
        return qualifiers(handle)
                .flatMap(at -> {
                    if (at instanceof HealthGroup hg) {
                        return Stream.of(hg);
                    }
                    if (at instanceof HealthGroups hgs) {
                        return Stream.of(hgs.value());
                    }
                    return Stream.empty();
                });
    }

    private <T> void addHealthResponses(Stream<? extends Instance.Handle<T>> stream, Predicate<Instance.Handle<?>> includes,
            Consumer<T> action) {
        stream.filter(includes).map(Instance.Handle::get).forEach(action);
    }

    private Collection<HealthRegistry> getRegistries(HealthType type, String group) {
        if (type != null) {
            return List.of(HealthRegistries.getRegistry(type));
        }
        switch (group) {
            case HG_WELL_OR_GROUP -> {
                List<HealthRegistry> result = new ArrayList<>();
                result.add(HealthRegistries.getRegistry(HealthType.WELLNESS));
                result.addAll(HealthRegistries.getHealthGroupRegistries());
                return result;
            }
            case HG_ALL -> {
                List<HealthRegistry> result = new ArrayList<>();
                result.add(HealthRegistries.getRegistry(HealthType.WELLNESS));
                result.add(HealthRegistries.getRegistry(HealthType.READINESS));
                result.add(HealthRegistries.getRegistry(HealthType.LIVENESS));
                result.add(HealthRegistries.getRegistry(HealthType.STARTUP));
                result.addAll(HealthRegistries.getHealthGroupRegistries());
                return result;
            }
            case HG_GROUP -> {
                return HealthRegistries.getHealthGroupRegistries();
            }
            default -> {
                return List.of(HealthRegistries.getHealthGroupRegistry(group));
            }
        }
    }

    private void addHealthResponses(Collection<HealthRegistry> registries, List<Uni<HealthCheckResponse>> result) {
        for (HealthRegistry registry : registries) {
            if (registry instanceof HealthRegistryImpl impl) {
                impl.getChecks(Map.of()).stream()
                        .map(uni -> withTimeout(registry.toString(), uni))
                        .forEach(result::add);
            }
        }
    }
}
