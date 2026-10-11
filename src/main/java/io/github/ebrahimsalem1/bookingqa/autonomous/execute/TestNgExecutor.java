package io.github.ebrahimsalem1.bookingqa.autonomous.execute;

import io.github.ebrahimsalem1.bookingqa.autonomous.ledger.RunLedger;
import io.github.ebrahimsalem1.bookingqa.autonomous.ledger.TestOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.TestNG;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.testng.xml.internal.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;

/**
 * Runs the regular TestNG suite in-process with the groups the planner chose, so the agent and a
 * plain {@code mvn test} execute exactly the same checks with the same listeners.
 */
public class TestNgExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(TestNgExecutor.class);
    private final String suiteResource;

    public TestNgExecutor(String suiteResource) {
        this.suiteResource = suiteResource;
    }

    /** @return outcomes recorded during this phase only */
    public List<TestOutcome> run(String phase, Collection<String> includeGroups, Collection<String> excludeGroups, String cucumberTags) {
        System.setProperty("qa.agent.phase", phase);
        if (cucumberTags == null || cucumberTags.isBlank()) {
            System.clearProperty("cucumber.filter.tags");
        } else {
            System.setProperty("cucumber.filter.tags", cucumberTags);
        }
        int before = RunLedger.snapshot().size();

        XmlSuite suite = loadSuite();
        suite.setName(suite.getName() + " [" + phase + "]");
        for (XmlTest test : suite.getTests()) {
            includeGroups.forEach(test::addIncludedGroup);
            excludeGroups.forEach(test::addExcludedGroup);
        }

        TestNG testng = new TestNG();
        testng.setUseDefaultListeners(false);
        testng.setXmlSuites(List.of(suite));
        testng.setOutputDirectory("target/autonomous/testng-" + phase);
        LOG.info("Phase '{}': include {} exclude {} cucumber tags '{}'", phase, includeGroups, excludeGroups, cucumberTags);
        testng.run();

        List<TestOutcome> all = RunLedger.snapshot();
        return all.subList(before, all.size());
    }

    private XmlSuite loadSuite() {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(suiteResource)) {
            if (in == null) {
                throw new IllegalStateException("Suite not found on classpath: " + suiteResource);
            }
            Collection<XmlSuite> suites = new Parser(in).parse();
            return suites.iterator().next();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
