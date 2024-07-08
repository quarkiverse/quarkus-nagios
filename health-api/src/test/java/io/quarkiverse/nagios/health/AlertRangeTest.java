package io.quarkiverse.nagios.health;

import org.junit.jupiter.api.Test;

import static io.quarkiverse.nagios.health.AlertRange.range;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AlertRangeTest {

    @Test
    void getExpression_defaultRange() {
        var range = range().build();
        assertExpression("", range);
    }

    @Test
    void getExpression_above() {
        var range = range().above(10);
        assertExpression("10", range);
    }

    @Test
    void getExpression_negativeOrAbove() {
        var range = range().negativeOrAbove(10);
        assertExpression("10", range);
    }

    @Test
    void getExpression_onlyAbove() {
        var range = range().onlyAbove(10);
        assertExpression("~:10", range);
    }

    @Test
    void getExpression_below() {
        var range = range().below(10);
        assertExpression("10:", range);
    }

    @Test
    void getExpression_outside() {
        var range = range().outside(10, 20);
        assertExpression("10:20", range);
    }

    @Test
    void getExpression_inside() {
        var range = range().inside(10, 20);
        assertExpression("@10:20", range);
    }

    @Test
    void getExpression_anyPositive() {
        var range = range().inside(0, Long.MAX_VALUE);
        assertExpression("@" + Long.MAX_VALUE, range);
    }

    @Test
    void getExpression_all() {
        var range = range().inside(Long.MIN_VALUE, Long.MAX_VALUE);
        assertExpression("@~:", range);
    }

    private static void assertExpression(String expected, AlertRange range) {
        assertEquals(expected, range.getExpression().orElse(""));

        var parsed = AlertRange.parse(expected);
        assertEquals(range.inside(), parsed.inside(), "inside");
        assertEquals(range.min(), parsed.min(), "min");
        assertEquals(range.max(), parsed.max(), "max");
    }
}
