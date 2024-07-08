package io.quarkiverse.nagios.deployment;

import io.quarkiverse.nagios.runtime.*;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.*;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.vertx.http.deployment.*;

class NagiosProcessor {

    private static final String FEATURE = "nagios";
    private static final String QUARKUS_NAGIOS_MANAGEMENT_ENABLED = "quarkus.nagios.management.enabled";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem reporter() {
        return AdditionalBeanBuildItem.unremovableOf(NagiosStatusReporter.class);
    }

    @BuildStep
    AdditionalBeanBuildItem atNagiosExcluded() {
        return new AdditionalBeanBuildItem(NagiosExcluded.class);
    }

    @BuildStep
    void routes(NagiosConfig config,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            BuildProducer<RouteBuildItem> routeProducer) {
        routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management(QUARKUS_NAGIOS_MANAGEMENT_ENABLED)
                .route(config.rootPath)
                .routeConfigKey("quarkus.nagios.root-path")
                .handler(new NagiosStatusRootHandler())
                .displayOnNotFoundPage()
                .blockingRoute()
                .build());
        routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management(QUARKUS_NAGIOS_MANAGEMENT_ENABLED)
                .nestedRoute(config.rootPath, "*")
                .handler(new NagiosStatusTypeHandler())
                .displayOnNotFoundPage()
                .blockingRoute()
                .build());
        routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management(QUARKUS_NAGIOS_MANAGEMENT_ENABLED)
                .nestedRoute(config.rootPath, "group/*")
                .handler(new NagiosStatusGroupHandler())
                .displayOnNotFoundPage()
                .blockingRoute()
                .build());
    }
}
