package io.github.ebrahimsalem1.bookingqa.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Test data that stays valid forever: stay dates are always relative to today (a hard-coded
 * date silently turns into a "past date" negative test a few months later) and destinations
 * come from {@code testdata/destinations.json}, not from test code.
 */
public final class TestData {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static List<Destination> destinations;

    private TestData() {
    }

    /** A check-in far enough ahead to have inventory, close enough to be bookable. */
    public static LocalDate checkin() {
        return LocalDate.now().plusDays(45);
    }

    public static LocalDate checkout(int nights) {
        return checkin().plusDays(nights);
    }

    public static synchronized List<Destination> destinations() {
        if (destinations == null) {
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("testdata/destinations.json")) {
                if (in == null) {
                    throw new IllegalStateException("testdata/destinations.json not on classpath");
                }
                destinations = MAPPER.readValue(in, new TypeReference<>() {
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return destinations;
    }

    public static Destination destination(String name) {
        return destinations().stream()
                .filter(d -> d.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown destination in testdata: " + name));
    }

    /**
     * @param name             what a user types, e.g. "Cairo"
     * @param expectedInLabel  text the top suggestion's label must contain, e.g. "Egypt"
     * @param demandCityId     Booking.com city id used by the Demand API, e.g. -290692
     */
    public record Destination(String name, String expectedInLabel, String countryCode, Integer demandCityId) {
    }
}
