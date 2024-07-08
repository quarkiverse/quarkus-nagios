package io.quarkiverse.nagios.health;

public interface NagiosPerformanceValue {

    String getLabel();

    long getValue();

    String getUnit();

    StringBuilder describeWarningExpression(StringBuilder sb);

    StringBuilder describeCriticalExpression(StringBuilder sb);

    default StringBuilder describeRecord(StringBuilder sb, NagiosStringBuilder buf) {
        return sb.append('\'')
                .append(buf.asLabel(getLabel()))
                .append("'=")
                .append(getValue())
                .append(buf.asUnit(getUnit()))
                .append(';')
                .append(buf.write(this::describeWarningExpression).asRange()).append(';')
                .append(buf.write(this::describeCriticalExpression).asRange());
    }
}
