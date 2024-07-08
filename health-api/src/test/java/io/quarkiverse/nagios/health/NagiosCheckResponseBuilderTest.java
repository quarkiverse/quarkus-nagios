package io.quarkiverse.nagios.health;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class NagiosCheckResponseBuilderTest {

    @Test
    void withCheck() {
        var response1 = NagiosCheckResponse.named("response 1")
                .withCheck(NagiosCheck.named("check 1").result(0))
                .build();
        var response2 = NagiosCheckResponse.named("response 2")
                .up().build();
        var response3 = NagiosCheckResponse.named("merged")
                .withChecks(List.of(response1, response2))
                .build();
        assertTrue(response3.toString().contains("2 checks passed"));
    }

    @Test
    void withSpecialCharacters() {
        var response1 = NagiosCheckResponse.named("My \n;|='\" Response 1")
                .withCheck(NagiosCheck.named("My \n;|='\" Check 1")
                        .unit("My \n;|='\" Unit 1")
                        .warningIf().above(1)
                        .result(2))
                .withCheck(NagiosCheck.named("My \n;|='\" Check 2")
                        .result("  My \n;|='\" Result  ", NagiosStatus.WARNING))
                .build();
        assertEquals("""
                WARNING: My ;/='" Check 1: 2; My ;/='" Check 2: My ;/='" Result|'My ;|:"" Check 1'=2My|Unit;1;
                My ;/='" Check 1: 2 [WARNING] (1;)
                My ;/='" Check 2: My ;/='" Result [WARNING]
                """,
                response1.toString());
    }
}
