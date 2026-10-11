package io.github.ebrahimsalem1.bookingqa.autonomous.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.ebrahimsalem1.bookingqa.config.QaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Optional LLM second opinion on a finished run (Anthropic Messages API).
 *
 * <p>Design constraints:
 * <ul>
 *   <li><b>Advisory only.</b> The verdict is computed by deterministic rules before the LLM is
 *       asked; nothing it says can turn a FAIL into a PASS.</li>
 *   <li><b>No secrets, no PII.</b> It receives the triage table and observations, which are
 *       already redacted (no headers with credentials, no bodies).</li>
 *   <li><b>Off by default when no key.</b> Without {@code ANTHROPIC_API_KEY} the agent is fully
 *       functional; the report simply has no AI section.</li>
 * </ul>
 */
public class LlmAdvisor {

    private static final Logger LOG = LoggerFactory.getLogger(LlmAdvisor.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            You are a senior SDET reviewing the results of an autonomous API test run against booking.com
            (public web endpoints and the Booking.com Demand API). You receive the deterministic triage of every
            non-passing check and the explorer's observations. Be concise and concrete. Respond in Markdown with:
            1. "Likely root causes": group related failures, say which look like real product defects versus
               environment/test issues, and how confident you are.
            2. "What to verify manually": at most 5 bullet points.
            3. "Next exploratory charters": at most 3, each as "Explore <target> with <resources> to discover <information>".
            Never invent data that is not in the input. If the input shows no failures, say so in one line and
            only propose charters.""";

    public Optional<String> advise(String runDigest) {
        QaConfig cfg = QaConfig.get();
        Optional<String> key = cfg.find("anthropic.api.key");
        if (!cfg.boolValue("llm.enabled") || key.isEmpty()) {
            LOG.info("LLM advisor disabled (llm.enabled={} key present={})", cfg.find("llm.enabled").orElse("?"), key.isPresent());
            return Optional.empty();
        }
        try {
            ObjectNode body = JSON.createObjectNode();
            body.put("model", cfg.require("llm.model"));
            body.put("max_tokens", cfg.intValue("llm.max.tokens"));
            body.put("system", SYSTEM_PROMPT);
            ArrayNode messages = body.putArray("messages");
            messages.addObject().put("role", "user").put("content", runDigest);

            HttpRequest request = HttpRequest.newBuilder(URI.create(cfg.require("llm.endpoint")))
                    .timeout(Duration.ofSeconds(90))
                    .header("content-type", "application/json")
                    .header("x-api-key", key.get())
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.warn("LLM advisor HTTP {}: {}", response.statusCode(), abbreviate(response.body()));
                return Optional.empty();
            }
            JsonNode content = JSON.readTree(response.body()).path("content");
            StringBuilder text = new StringBuilder();
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    text.append(block.path("text").asText());
                }
            }
            return text.isEmpty() ? Optional.empty() : Optional.of(text.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            LOG.warn("LLM advisor unavailable: {}", e.toString());
            return Optional.empty();
        }
    }

    private static String abbreviate(String s) {
        return s == null ? "" : (s.length() > 300 ? s.substring(0, 300) + "..." : s);
    }
}
