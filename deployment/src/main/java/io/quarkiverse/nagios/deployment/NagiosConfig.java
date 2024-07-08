package io.quarkiverse.nagios.deployment;

import io.quarkus.runtime.annotations.*;

@ConfigRoot(name = "nagios")
public class NagiosConfig {

    /**
     * Root path
     */
    @ConfigItem(defaultValue = "nagios")
    String rootPath;

    /**
     * Default group
     */
    @ConfigItem(defaultValue = "/well/or/group/")
    String defaultGroup;

    /**
     * Management enabled
     */
    @ConfigItem(name = "management.enabled", defaultValue = "true")
    boolean managementEnabled;
}
