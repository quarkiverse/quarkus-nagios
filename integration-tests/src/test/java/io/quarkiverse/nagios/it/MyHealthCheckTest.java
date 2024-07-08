package io.quarkiverse.nagios.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

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
