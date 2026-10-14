package fr.exemple.adresses;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class SearchRateLimiterTest {

    // Vérifie la limite par adresse et sa remise à zéro après la fenêtre définie.
    @Test
    void limiteChaqueAdresseJusquaLaFinDeSaFenetre() {
        // Given
        AtomicLong ticker = new AtomicLong();
        SearchRateLimiter limiter = new SearchRateLimiter(2, Duration.ofSeconds(10), 10, ticker::get);

        // When
        boolean firstRequestAllowed = limiter.allow("192.0.2.1");
        boolean secondRequestAllowed = limiter.allow("192.0.2.1");
        boolean thirdRequestAllowed = limiter.allow("192.0.2.1");
        boolean otherClientAllowed = limiter.allow("192.0.2.2");
        ticker.set(Duration.ofSeconds(10).toNanos());
        boolean requestAfterWindowAllowed = limiter.allow("192.0.2.1");

        // Then
        assertTrue(firstRequestAllowed);
        assertTrue(secondRequestAllowed);
        assertFalse(thirdRequestAllowed);
        assertTrue(otherClientAllowed);
        assertTrue(requestAfterWindowAllowed);
    }
}