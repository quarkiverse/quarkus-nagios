package io.quarkiverse.nagios.it;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class MyHealthCheckTest {

    @Test
    void wellness() {
        given()
                .when()
                .get("q/nagios")
                .then()
                .statusCode(200)
                .body(containsString("WARNING: My Check"));
    }

    @Test
    void slow() {
        given()
                .when()
                .get("q/nagios/live")
                .then()
                .statusCode(200)
                .body(containsString("OK: 2 checks passed"));
    }
}
