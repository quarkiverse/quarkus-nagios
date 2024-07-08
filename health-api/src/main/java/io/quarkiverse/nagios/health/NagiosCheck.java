package io.quarkiverse.nagios.health;

import java.util.Map;

public record NagiosCheck(
        String name,
        String unit,
        AlertRange warningRange,
        AlertRange criticalRange,
        boolean exportPerformance) {

    public static NagiosCheckBuilder named(String name) {
        return new NagiosCheckBuilder().name(name);
    }

    public NagiosCheckResult result(long value) {
        return new NagiosLongResult(name, value, unit, warningRange, criticalRange, Map.of(), exportPerformance);
    }

    public NagiosCheckResult result(long value, NagiosStatus status) {
        return new NagiosLongResult(name, value, unit, status, warningRange, criticalRange, Map.of(), exportPerformance);
    }

    public NagiosCheckResult result(NagiosStatus status) {
        if (exportPerformance) {
            return result(0, status);
        }
        return new NagiosValueResult(name, status, Map.of());
    }

    public NagiosCheckResult result(Object value, NagiosStatus status) {
        if (exportPerformance) {
            return result(((Number) value).longValue(), status);
        }
        return new NagiosValueResult(name, value, status, Map.of());
    }

    public NagiosCheckResult result(Throwable t) {
        return result(t, NagiosStatus.CRITICAL);
    }

    public NagiosCheckResult result(Throwable t, NagiosStatus status) {
        var clazz = t.getClass().getName();
        var message = getMessage(t);
        if (message == null) {
            return new NagiosValueResult(name, clazz, status, Map.of());
        }
        return new NagiosValueResult(name, clazz + ": " + message, status,
                Map.of("exception", clazz, "message", message));
    }

    private static String getMessage(Throwable t) {
        if (t == null)
            return null;
        var message = t.getMessage();
        return message != null ? message : getMessage(t.getCause());
    }
}
