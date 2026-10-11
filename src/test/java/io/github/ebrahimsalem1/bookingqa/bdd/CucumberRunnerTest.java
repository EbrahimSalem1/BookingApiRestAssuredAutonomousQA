package io.github.ebrahimsalem1.bookingqa.bdd;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import io.qameta.allure.Epic;
import org.testng.annotations.DataProvider;

/**
 * Business-readable layer over the same clients the TestNG checks use. Scenario tags mirror the
 * TestNG groups (@web, @autocomplete, @demand-auth, @demand-partner), so the autonomous planner
 * can exclude a blocked surface from both layers with one decision.
 */
@Epic("BDD scenarios")
@CucumberOptions(
        features = "classpath:features",
        glue = "io.github.ebrahimsalem1.bookingqa.bdd",
        plugin = {"pretty", "summary",
                "json:target/cucumber/cucumber.json",
                "html:target/cucumber/index.html"},
        monochrome = true)
public class CucumberRunnerTest extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = false)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
