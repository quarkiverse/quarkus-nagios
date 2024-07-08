package io.quarkiverse.nagios.health;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

public record AlertRange(
        long min,
        long max,
        boolean inside) {

    public static final AlertRange ALLOW_ALL = new AlertRange(Long.MIN_VALUE, Long.MAX_VALUE, false);
    public static final AlertRange ALLOW_ZERO = new AlertRange(0, 0, false);
    public static final AlertRange ALLOW_POSITIVE = new AlertRange(0, Long.MAX_VALUE, false);

    public AlertRange(long max) {
        this(0, max, false);
    }

    public AlertRange {
        if (min > max)
            throw new IllegalArgumentException(min + " > " + max);
    }

    public boolean alert(long value) {
        return inside ^ (value < min || max < value);
    }

    public StringBuilder describeExpression(StringBuilder sb) {
        if (inside) {
            sb.append('@');
        }
        if (min == Long.MIN_VALUE) {
            sb.append("~:");
        } else if (min != 0) {
            sb.append(min).append(':');
        }
        if (max != Long.MAX_VALUE || (inside && min == 0)) {
            return sb.append(max);
        }
        return sb;
    }

    public Optional<String> getExpression() {
        var sb = describeExpression(new StringBuilder());
        return sb.isEmpty() ? Optional.empty() : Optional.of(sb.toString());
    }

    public NagiosStatus getStatus(long value, AlertRange warningRange, AlertRange criticalRange) {
        if (criticalRange.alert(value))
            return NagiosStatus.CRITICAL;
        if (warningRange.alert(value))
            return NagiosStatus.WARNING;
        return NagiosStatus.OK;
    }

    public static AlertRange parse(String input) {
        var matcher = PATTERN.matcher(input);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid alert range: " + input);
        }
        var inside = matcher.group(1) != null;
        var gMin = matcher.group(3);
        var gMax = matcher.group(4);
        var min = 0L;
        if (gMin != null) {
            min = gMin.equals("~") ? Long.MIN_VALUE : Long.parseLong(gMin);
        }
        var max = gMax == null ? Long.MAX_VALUE : Long.parseLong(gMax);
        return new AlertRange(min, max, inside);
    }

    public static AlertRange.Builder<AlertRange> range() {
        return new AlertRange.Builder<>(Function.identity());
    }

    private static final Pattern PATTERN = Pattern.compile("^\\s*+(@\\s*+)?((~|-?\\d++)\\s*+:\\s*+)?(-?\\d++)?\\s*+$");

    public static class Builder<T> {

        private long min = 0;
        private long max = Long.MAX_VALUE;
        private boolean inside = false;

        private final Function<AlertRange, T> setter;

        public Builder(Function<AlertRange, T> setter) {
            this.setter = setter;
        }

        public Builder<T> min(long min) {
            this.min = min;
            return this;
        }

        public Builder<T> max(long max) {
            this.max = max;
            return this;
        }

        public T negativeOrAbove(long max) {
            return outside(0, max);
        }

        public T onlyAbove(long max) {
            return outside(Long.MIN_VALUE, max);
        }

        public T above(long max) {
            return outside(min, max);
        }

        public T below(long min) {
            return outside(min, Long.MAX_VALUE);
        }

        public T inside(long min, long max) {
            this.inside = true;
            this.min = min;
            this.max = max;
            return build();
        }

        public T outside(long min, long max) {
            this.inside = false;
            this.min = min;
            this.max = max;
            return build();
        }

        public T build() {
            return setter.apply(new AlertRange(min, max, inside));
        }
    }
}
