package io.github.ebrahimsalem1.bookingqa.tests.web;

import io.github.ebrahimsalem1.bookingqa.base.BaseApiTest;
import io.github.ebrahimsalem1.bookingqa.core.http.SpecFactory;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.ebrahimsalem1.bookingqa.core.guard.EnvironmentGuard.assumeNotBlocked;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Transport and header hygiene of the front door. These protect every user session
 * (including the checkout), so a regression here is a security incident, not a cosmetic bug.
 */
@Epic("Platform")
@Feature("Transport security")
@Test(groups = {"web", "security"})
public class WebPlatformSecurityTest extends BaseApiTest {

    private static final Pattern MAX_AGE = Pattern.compile("max-age=(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION_DISCLOSURE = Pattern.compile("\\d+\\.\\d+");

    private Response frontDoor() {
        return assumeNotBlocked(given().spec(SpecFactory.web(false)).when().get("/").andReturn());
    }

    @Test(groups = "smoke", description = "HSTS is enforced for at least one year")
    public void strictTransportSecurityIsEnforced() {
        String hsts = frontDoor().getHeader("Strict-Transport-Security");
        assertThat(hsts).as("Strict-Transport-Security header").isNotBlank();
        Matcher m = MAX_AGE.matcher(hsts);
        assertThat(m.find()).as("max-age directive in '%s'", hsts).isTrue();
        assertThat(Long.parseLong(m.group(1))).as("HSTS max-age seconds").isGreaterThanOrEqualTo(31_536_000L);
    }

    @Test(description = "Pages cannot be framed by other origins (clickjacking)")
    public void framingIsRestricted() {
        Response r = frontDoor();
        String xfo = r.getHeader("X-Frame-Options");
        String csp = r.getHeader("Content-Security-Policy");
        boolean protectedByXfo = xfo != null && !xfo.isBlank();
        boolean protectedByCsp = csp != null && csp.contains("frame-ancestors");
        assertThat(protectedByXfo || protectedByCsp)
                .as("X-Frame-Options='%s' or CSP frame-ancestors present", xfo)
                .isTrue();
    }

    @Test(description = "Server software version is not disclosed")
    public void serverVersionIsNotDisclosed() {
        String server = frontDoor().getHeader("Server");
        if (server != null) {
            assertThat(VERSION_DISCLOSURE.matcher(server).find()).as("version number in Server header '%s'", server).isFalse();
        }
    }

    @Test(description = "Plain HTTP is redirected to HTTPS")
    public void plainHttpRedirectsToHttps() {
        String base = config.webBaseUrl();
        if (!base.startsWith("https://")) {
            throw new SkipException("[NOT_APPLICABLE] HTTP->HTTPS redirect only meaningful against the real host (base " + base + ")");
        }
        Response r = given().spec(SpecFactory.web(false))
                .baseUri(base.replaceFirst("^https://", "http://"))
                .when().get("/").andReturn();
        assumeNotBlocked(r);
        assertThat(r.getStatusCode()).as("status of http:// front door").isIn(301, 302, 307, 308);
        assertThat(r.getHeader("Location")).as("redirect target").startsWith("https://");
    }
}
