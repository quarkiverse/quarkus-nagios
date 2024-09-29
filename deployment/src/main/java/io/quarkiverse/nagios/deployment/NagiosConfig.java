package io.quarkiverse.nagios.deployment;

import io.quarkus.runtime.annotations.*;
import io.smallrye.config.*;

@ConfigMapping(prefix = "quarkus.nagios")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface NagiosConfig {

    /**
     * Root path
     */
    @WithDefault("nagios")
    String rootPath();

    /**
     * Default group
     */
    @WithDefault("/well/or/group/")
    String defaultGroup();

    /**
     * Management configuration
     */
    Management management();

    interface Management {

        /**
         * Management enabled
         */
        @WithDefault("true")
        boolean enabled();
    }
}
