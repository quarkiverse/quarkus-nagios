package io.quarkiverse.nagios.health;

import java.util.*;

public record NagiosLongResult(
        String name,
        long value,
        String unit,
        NagiosStatus status,
        AlertRange warningRange,
        AlertRange criticalRange,
        Map<String, Object> data,
        boolean exportPerformance

) implements NagiosCheckResult, NagiosPerformanceValue {

    public NagiosLongResult(String name, long value, String unit, AlertRange warningRange, AlertRange criticalRange,
            Map<String, Object> data, boolean exportPerformance) {
        this(name, value, unit, AlertRange.ALLOW_ALL.getStatus(value, warningRange, criticalRange), warningRange, criticalRange,
                data, exportPerformance);
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
        sb.append(value).append(" [").append(status).append("] (");
        describeWarningExpression(sb).append(";");
        return describeCriticalExpression(sb).append(")");
    }

    @Override
    public Map<String, Object> getData() {
        return data;
    }

    @Override
    public List<NagiosPerformanceValue> getPerformanceValues() {
        return exportPerformance ? List.of(this) : List.of();
    }

    @Override
    public String getLabel() {
        return name;
    }

    @Override
    public long getValue() {
        return value;
    }

    @Override
    public String getUnit() {
        return unit;
    }

    @Override
    public StringBuilder describeWarningExpression(StringBuilder sb) {
        return warningRange.describeExpression(sb);
    }

    @Override
    public StringBuilder describeCriticalExpression(StringBuilder sb) {
        return criticalRange.describeExpression(sb);
    }
}
