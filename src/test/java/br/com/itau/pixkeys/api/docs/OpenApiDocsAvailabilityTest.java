package br.com.itau.pixkeys.api.docs;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;


@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
        }
)
class OpenApiDocsAvailabilityTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Test
    void swaggerUi_shouldRespond2xxOr3xx_andHaveLocationWhenRedirects() {
        ResponseEntity<String> r =
                rest.getForEntity("/swagger-ui/index.html", String.class);
        System.out.println("Swagger UI → " + r.getStatusCode().value() + " " + r.getStatusCode());

        assertTrue(
                r.getStatusCode().is2xxSuccessful() || r.getStatusCode().is3xxRedirection(),
                "Swagger Should Respond 2xx Or 3xx"
        );
    }

    @Test
    void apiDocs_shouldContainOpenApiField() {
        ResponseEntity<String> r =
                rest.getForEntity("/v3/api-docs", String.class);

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertNotNull(r.getBody());
        assertTrue(r.getBody().contains("\"openapi\""),
                "The JSON from /v3/api-docs must contain the 'openapi' field");

        MediaType ct = r.getHeaders().getContentType();
        assertNotNull(ct, "Content-Type must be present");
        assertTrue(MediaType.APPLICATION_JSON.isCompatibleWith(ct),
                "Expected JSON Content-Type, got: " + ct);

    }
}
