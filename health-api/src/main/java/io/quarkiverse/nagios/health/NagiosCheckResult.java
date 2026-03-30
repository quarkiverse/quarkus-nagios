package io.quarkiverse.nagios.health;

import java.util.*;

public interface NagiosCheckResult {

    String getName();

    NagiosStatus getNagiosStatus();

    StringBuilder describeResult(StringBuilder sb);

    StringBuilder describeStatus(StringBuilder sb);

    default String getStatusString() {
        return describeStatus(new StringBuilder()).toString();
    }

    Map<String, Object> getData();

    List<NagiosPerformanceValue> getPerformanceValues();

    default NagiosCheckResponse asResponse() {
        return NagiosCheckResponse.named(getName()).withCheck(this).build();
    }

    NagiosCheckResult withData(Map<String, Object> data);

    default NagiosCheckResult addData(Map<String, Object> data) {
        var copy = new HashMap<>(getData());
        copy.putAll(data);
        return withData(copy);
    }

    default NagiosCheckResult addData(String key, Object value) {
        return addData(Map.of(key, value));
    }

    default NagiosCheckResult addData(String key1, Object value1, String key2, Object value2) {
        return addData(Map.of(key1, value1, key2, value2));
    }
}
