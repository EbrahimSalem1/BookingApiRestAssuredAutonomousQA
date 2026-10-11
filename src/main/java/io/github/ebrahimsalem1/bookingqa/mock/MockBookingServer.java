package io.github.ebrahimsalem1.bookingqa.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * In-process WireMock that serves contract-faithful stand-ins for every booking.com surface
 * this project touches. Stubs live in {@code src/test/resources/wiremock/mappings}.
 *
 * <p>Started lazily (first time a {@code {mock}} URL is resolved) on a free port and stopped by
 * a JVM shutdown hook, so it works the same from Maven, the IDE and the autonomous agent.
 */
public final class MockBookingServer {

    private static final Logger LOG = LoggerFactory.getLogger(MockBookingServer.class);
    private static WireMockServer server;

    private MockBookingServer() {
    }

    /** @return base URL, e.g. {@code http://localhost:53122} */
    public static synchronized String ensureStarted() {
        if (server == null || !server.isRunning()) {
            server = new WireMockServer(options()
                    .dynamicPort()
                    .usingFilesUnderClasspath("wiremock")
                    .disableRequestJournal());
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(MockBookingServer::stop, "wiremock-stop"));
            LOG.info("Mock booking.com started on {} with {} stubs", server.baseUrl(), server.getStubMappings().size());
        }
        return server.baseUrl();
    }

    public static synchronized void stop() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }

    public static synchronized boolean isRunning() {
        return server != null && server.isRunning();
    }
}
