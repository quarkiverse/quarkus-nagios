package io.quarkiverse.nagios.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NagiosCheckResultTest {

    @Test
    void warning_above() {
        var key = NagiosCheck.named("test").warningIf().above(10).build();
        var ok = key.result(10);
        var warn = key.result(11);
        assertEquals(NagiosStatus.OK, ok.getNagiosStatus());
        assertEquals(NagiosStatus.WARNING, warn.getNagiosStatus());
        assertEquals("10", key.warningRange().getExpression().orElse(""));
    }
}
