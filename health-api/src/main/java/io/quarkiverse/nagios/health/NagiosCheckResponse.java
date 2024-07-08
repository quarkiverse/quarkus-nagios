package io.quarkiverse.nagios.health;

import java.util.*;

import org.eclipse.microprofile.health.HealthCheckResponse;

public class NagiosCheckResponse extends HealthCheckResponse {

    public static NagiosCheckResponseBuilder named(String name) {
        return new NagiosCheckResponseBuilder().name(name);
    }

    private final NagiosStatus status;
    private final List<NagiosCheckResult> checks;
    private final List<NagiosPerformanceValue> performance;

    public NagiosCheckResponse(String name, NagiosStatus status, List<NagiosCheckResult> checks,
            List<NagiosPerformanceValue> performance, Map<String, Object> data) {
        super(name, status.toHealth(),
                (data == null || data.isEmpty()) ? Optional.empty() : Optional.of(Collections.unmodifiableMap(data)));
        this.status = status;
        this.checks = (checks == null || checks.isEmpty()) ? List.of() : Collections.unmodifiableList(checks);
        this.performance = (performance == null || performance.isEmpty()) ? List.of()
                : Collections.unmodifiableList(performance);
    }

    public NagiosStatus getNagiosStatus() {
        return status;
    }

    public List<NagiosCheckResult> getChecks() {
        return checks;
    }

    public List<NagiosPerformanceValue> getPerformanceValues() {
        return performance;
    }

    public Map<String, Object> getDataMap() {
        return getData().orElse(Map.of());
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        var buf = new NagiosStringBuilder();
        sb.append(status)
                .append(": ")
                .append(buf.write(this::describeInfo).asInfo())
                .append('|');
        performance.stream()
                .sorted(Comparator.comparing(NagiosPerformanceValue::getLabel))
                .forEach(p -> p.describeRecord(sb, buf).append(' '));
        sb.setCharAt(sb.length() - 1, '\n');
        getDataMap().forEach((key, value) -> sb.append(buf.asOutputLine(key))
                .append(": ")
                .append(buf.asOutputLine(value))
                .append('\n'));
        return sb.toString();
    }

    private StringBuilder describeInfo(StringBuilder sb) {
        if (status == NagiosStatus.OK) {
            if (checks.size() == 1)
                return sb.append("1 check passed");
            return sb.append(checks.size()).append(" checks passed");
        }
        var matching = checks.stream().filter(r -> r.getNagiosStatus() == status).toList();
        if (matching.isEmpty()) {
            return sb.append(getName());
        }
        return describeProblems(sb, matching);
    }

    private static StringBuilder describeProblems(StringBuilder sb, List<NagiosCheckResult> matching) {
        for (int i = 0; i < matching.size() && i < 3; i++) {
            matching.get(i).describeResult(sb).append("; ");
        }
        if (matching.size() > 3) {
            return sb.append(matching.size() - 3).append(" more");
        }
        sb.setLength(sb.length() - 2);
        return sb;
    }
}
