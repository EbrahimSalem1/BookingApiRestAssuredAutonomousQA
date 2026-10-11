package io.github.ebrahimsalem1.bookingqa.base;

import io.github.ebrahimsalem1.bookingqa.config.QaConfig;
import io.restassured.module.jsv.JsonSchemaValidator;
import org.hamcrest.Matcher;
import org.testng.annotations.BeforeClass;

/**
 * Common plumbing for every API check. Deliberately thin: behaviour lives in clients, guards and
 * assertions, not in an inheritance tree.
 */
public abstract class BaseApiTest {

    protected QaConfig config;

    @BeforeClass(alwaysRun = true)
    public void initConfig() {
        config = QaConfig.get();
    }

    /** JSON schema stored under {@code src/test/resources/schemas}. */
    protected static Matcher<?> matchesSchema(String relativePath) {
        return JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/" + relativePath);
    }
}
