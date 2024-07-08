package io.quarkiverse.nagios.runtime;

import io.vertx.ext.web.RoutingContext;

public class NagiosStatusGroupHandler extends NagiosStatusRootHandler {

    @Override
    protected String getGroup(RoutingContext context) {
        String path = context.normalizedPath();
        int end = path.length();
        int start = path.lastIndexOf('/');
        if (start + 1 == path.length()) {
            end--;
            start = path.lastIndexOf('/', start - 1);
        }
        if (start < 0)
            return "";
        return path.substring(start + 1, end);
    }
}
