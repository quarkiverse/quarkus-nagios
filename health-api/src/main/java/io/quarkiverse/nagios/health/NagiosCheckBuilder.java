package io.quarkiverse.nagios.health;

public class NagiosCheckBuilder {

    private String name = "health";
    private String unit = "";
    private AlertRange warningRange = AlertRange.ALLOW_POSITIVE;
    private AlertRange criticalRange = AlertRange.ALLOW_POSITIVE;
    private boolean exportPerformance = false;

    public NagiosCheckBuilder name(String name) {
        this.name = name;
        return this;
    }

    public NagiosCheckBuilder performance() {
        this.exportPerformance = true;
        return this;
    }

    public NagiosCheckBuilder unit(String unit) {
        this.exportPerformance = true;
        this.unit = unit;
        return this;
    }

    public NagiosCheckBuilder warning(AlertRange warningRange) {
        this.warningRange = warningRange;
        return this;
    }

    public NagiosCheckBuilder critical(AlertRange criticalRange) {
        this.criticalRange = criticalRange;
        return this;
    }

    public AlertRange.Builder<NagiosCheckBuilder> warningIf() {
        return new AlertRange.Builder<>(this::warning);
    }

    public AlertRange.Builder<NagiosCheckBuilder> criticalIf() {
        return new AlertRange.Builder<>(this::critical);
    }

    public NagiosCheck build() {
        return new NagiosCheck(name, unit, warningRange, criticalRange, exportPerformance);
    }

    public NagiosCheckResult result(long value) {
        return build().result(value);
    }

    public NagiosCheckResult result(long value, NagiosStatus status) {
        return build().result(value, status);
    }

    public NagiosCheckResult result(NagiosStatus status) {
        return build().result(status);
    }

    public NagiosCheckResult result(Object value, NagiosStatus status) {
        return build().result(value, status);
    }
}
