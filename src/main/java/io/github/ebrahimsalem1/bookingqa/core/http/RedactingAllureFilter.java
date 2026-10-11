package io.github.ebrahimsalem1.bookingqa.core.http;

import io.qameta.allure.Allure;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Attaches every request/response pair to the Allure report and the debug log, with secrets
 * (Authorization, affiliate id, cookies) redacted so reports can be shared safely.
 * Bodies are truncated so a 1 MB HTML search page does not bloat the report.
 */
public final class RedactingAllureFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(RedactingAllureFilter.class);
    private static final Set<String> SECRET_HEADERS = Set.of("authorization", "x-affiliate-id", "cookie", "set-cookie", "x-api-key");
    private static final int MAX_BODY_CHARS = 8_000;

    @Override
    public Response filter(FilterableRequestSpecification req, FilterableResponseSpecification resSpec, FilterContext ctx) {
        Response response = ctx.next(req, resSpec);

        String request = req.getMethod() + " " + req.getURI() + "\n"
                + req.getHeaders().asList().stream().map(RedactingAllureFilter::render).collect(Collectors.joining("\n"))
                + "\n\n" + truncate(req.getBody() == null ? "" : String.valueOf(req.<Object>getBody()));

        String reply = "HTTP " + response.getStatusCode() + "  (" + response.getTime() + " ms)\n"
                + response.getHeaders().asList().stream().map(RedactingAllureFilter::render).collect(Collectors.joining("\n"))
                + "\n\n" + truncate(response.getBody().asString());

        LOG.debug("--> {}\n<-- {}", request, reply);
        try {
            Allure.addAttachment("Request: " + req.getMethod() + " " + req.getDerivedPath(), "text/plain", request, ".txt");
            Allure.addAttachment("Response: " + response.getStatusCode(), "text/plain", reply, ".txt");
        } catch (RuntimeException ignored) {
            // Outside an Allure test context (e.g. the explorer) attachments are not possible: fine.
        }
        return response;
    }

    private static String render(Header h) {
        boolean secret = SECRET_HEADERS.contains(h.getName().toLowerCase(Locale.ROOT));
        return h.getName() + ": " + (secret ? "***redacted***" : h.getValue());
    }

    private static String truncate(String s) {
        return s.length() <= MAX_BODY_CHARS ? s : s.substring(0, MAX_BODY_CHARS) + "\n... [truncated " + (s.length() - MAX_BODY_CHARS) + " chars]";
    }
}
