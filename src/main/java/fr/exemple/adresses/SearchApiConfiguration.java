package fr.exemple.adresses;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

record SearchApiConfiguration(String apiUrl, Duration connectTimeout, Duration requestTimeout) {

    private static final long MIN_TIMEOUT_SECONDS = 1;

    static SearchApiConfiguration load() {
        Properties defaults = new Properties();
        try (InputStream input = SearchApiConfiguration.class.getResourceAsStream("search-api.properties")) {
            if (input == null) {
                throw new IllegalStateException("Missing search-api.properties");
            }
            defaults.load(input);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read search-api.properties", e);
        }

        return from(defaults, System.getenv(), System.getProperties());
    }

    static SearchApiConfiguration from(
            Properties defaults,
            Map<String, String> environment,
            Properties systemProperties) {
        return new SearchApiConfiguration(
                value(defaults, environment, systemProperties, "ban.api.url", "BAN_API_URL"),
                timeout(defaults, environment, systemProperties,
                        "ban.api.connect-timeout-seconds", "BAN_API_CONNECT_TIMEOUT_SECONDS"),
                timeout(defaults, environment, systemProperties,
                        "ban.api.request-timeout-seconds", "BAN_API_REQUEST_TIMEOUT_SECONDS"));
    }

    SearchApiConfiguration withApiUrl(String apiUrl) {
        return new SearchApiConfiguration(apiUrl, connectTimeout, requestTimeout);
    }

    private static Duration timeout(
            Properties defaults,
            Map<String, String> environment,
            Properties systemProperties,
            String propertyName,
            String environmentName) {
        String configuredValue = value(defaults, environment, systemProperties, propertyName, environmentName);
        try {
            long seconds = Long.parseLong(configuredValue);
            if (seconds < MIN_TIMEOUT_SECONDS) {
                throw new IllegalStateException(propertyName + " must be greater than zero");
            }
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(propertyName + " must be a positive number", e);
        }
    }

    private static String value(
            Properties defaults,
            Map<String, String> environment,
            Properties systemProperties,
            String propertyName,
            String environmentName) {
        String value = systemProperties.getProperty(propertyName);
        if (value != null && !value.isBlank()) {
            return value;
        }

        value = environment.get(environmentName);
        if (value != null && !value.isBlank()) {
            return value;
        }

        value = defaults.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing configuration property " + propertyName);
        }
        return value;
    }

    SearchApiConfiguration {
        URI uri = URI.create(apiUrl);
        if (!uri.isAbsolute() || uri.getHost() == null
                || (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("BAN API URL must be an absolute HTTP or HTTPS URL with a host");
        }
        if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()
                || requestTimeout == null || requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("API timeouts must be greater than zero");
        }
    }
}