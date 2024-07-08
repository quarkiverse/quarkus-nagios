package io.quarkiverse.nagios.health;

import java.util.*;

public record NagiosValueResult(
        String name,
        Object value,
        NagiosStatus status,
        Map<String, Object> data

) implements NagiosCheckResult {

    public NagiosValueResult(String name, NagiosStatus status, Map<String, Object> data) {
        this(name, status, status, data);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NagiosStatus getNagiosStatus() {
        return status;
    }

    @Override
    public StringBuilder describeResult(StringBuilder sb) {
        return sb.append(name).append(": ").append(value);
    }

    @Override
    public StringBuilder describeStatus(StringBuilder sb) {
        if (value == status) {
            return sb.append(status);
        }
        return sb.append(value).append(" [").append(status).append("]");
    }

    @Override
    public Map<String, Object> getData() {
        return data;
    }

    @Override
    public List<NagiosPerformanceValue> getPerformanceValues() {
        return List.of();
    }
}
