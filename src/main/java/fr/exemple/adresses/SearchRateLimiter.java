package fr.exemple.adresses;

import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

final class SearchRateLimiter {

    private static final int DEFAULT_MAXIMUM_REQUESTS = 30;
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(1);
    private static final int DEFAULT_MAXIMUM_TRACKED_CLIENTS = 4096;

    private final int maximumRequests;
    private final long windowNanos;
    private final int maximumTrackedClients;
    private final LongSupplier ticker;
    private final Map<String, Window> clients = new LinkedHashMap<>(16, 0.75f, true);

    SearchRateLimiter() {
        this(DEFAULT_MAXIMUM_REQUESTS, DEFAULT_WINDOW, DEFAULT_MAXIMUM_TRACKED_CLIENTS, System::nanoTime);
    }

    SearchRateLimiter(int maximumRequests, Duration window, int maximumTrackedClients, LongSupplier ticker) {
        if (maximumRequests < 1 || window == null || window.isZero() || window.isNegative()
                || maximumTrackedClients < 1 || ticker == null) {
            throw new IllegalArgumentException("Rate limit settings must be positive");
        }
        this.maximumRequests = maximumRequests;
        this.windowNanos = window.toNanos();
        this.maximumTrackedClients = maximumTrackedClients;
        this.ticker = ticker;
    }

    synchronized boolean allow(String clientAddress) {
        long now = ticker.getAsLong();
        String key = clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress;
        Window window = clients.get(key);

        if (window != null && now - window.startedAtNanos < windowNanos) {
            if (window.requests >= maximumRequests) {
                return false;
            }
            window.requests++;
            return true;
        }

        removeExpired(now);
        if (clients.size() >= maximumTrackedClients) {
            Iterator<String> oldestClient = clients.keySet().iterator();
            oldestClient.next();
            oldestClient.remove();
        }
        clients.put(key, new Window(now));
        return true;
    }

    private void removeExpired(long now) {
        Iterator<Window> windows = clients.values().iterator();
        while (windows.hasNext()) {
            if (now - windows.next().startedAtNanos >= windowNanos) {
                windows.remove();
            }
        }
    }

    private static final class Window {
        private final long startedAtNanos;
        private int requests = 1;

        private Window(long startedAtNanos) {
            this.startedAtNanos = startedAtNanos;
        }
    }
}