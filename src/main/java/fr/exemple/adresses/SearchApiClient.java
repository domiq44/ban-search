package fr.exemple.adresses;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

final class SearchApiClient {

    private static final Logger LOGGER = Logger.getLogger(SearchApiClient.class.getName());
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ObjectWriter PRETTY_JSON_WRITER = JSON_MAPPER.writerWithDefaultPrettyPrinter();

    private final SearchApiConfiguration configuration;
    private final HttpClient client;
    private final boolean prettyJsonLogs;

    SearchApiClient() {
        this(SearchApiConfiguration.load(), configuredPrettyJsonLogs());
    }

    SearchApiClient(String apiUrl) {
        this(SearchApiConfiguration.load().withApiUrl(apiUrl), configuredPrettyJsonLogs());
    }

    SearchApiClient(String apiUrl, boolean prettyJsonLogs) {
        this(SearchApiConfiguration.load().withApiUrl(apiUrl), prettyJsonLogs);
    }

    SearchApiClient(SearchApiConfiguration configuration) {
        this(configuration, configuredPrettyJsonLogs());
    }

    SearchApiClient(SearchApiConfiguration configuration, boolean prettyJsonLogs) {
        this.configuration = configuration;
        this.prettyJsonLogs = prettyJsonLogs;
        this.client = HttpClient.newBuilder()
                .connectTimeout(configuration.connectTimeout())
                .build();
    }

    HttpResponse<String> search(SearchCriteria criteria) throws IOException, InterruptedException {
        HttpRequest apiReq = HttpRequest.newBuilder(URI.create(buildApiUrl(configuration.apiUrl(), criteria)))
                .timeout(configuration.requestTimeout())
                .header("Accept", "application/json")
                .GET().build();
        long startedAt = System.nanoTime();
        try {
            HttpResponse<String> response = client.send(apiReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            LOGGER.info("BAN API response query=" + escapeLogValue(criteria.query())
                    + " status=" + response.statusCode()
                    + " durationMs=" + elapsedMillis(startedAt)
                    + " body=" + formatResponseBodyForLog(response.body()));
            return response;
        } catch (IOException | InterruptedException e) {
            LOGGER.warning("BAN API request failed query=" + escapeLogValue(criteria.query())
                    + " type=" + e.getClass().getSimpleName()
                    + " durationMs=" + elapsedMillis(startedAt));
            throw e;
        }
    }

    private String formatResponseBodyForLog(String body) {
        if (prettyJsonLogs) {
            try {
                JsonNode json = JSON_MAPPER.readTree(body);
                return json == null ? escapeLogValue(body) : PRETTY_JSON_WRITER.writeValueAsString(json);
            } catch (JsonProcessingException e) {
                return escapeLogValue(body);
            }
        }
        return escapeLogValue(body);
    }

    private static boolean configuredPrettyJsonLogs() {
        String value = System.getProperty("ban.api.log-json-pretty");
        if (value == null || value.isBlank()) {
            value = System.getenv("BAN_API_LOG_JSON_PRETTY");
        }
        return Boolean.parseBoolean(value);
    }

    private static String escapeLogValue(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (char character : value.toCharArray()) {
            if (Character.isISOControl(character)) {
                escaped.append(String.format("\\u%04x", (int) character));
            } else {
                escaped.append(character);
            }
        }
        return escaped.toString();
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    static String buildApiUrl(String base, SearchCriteria criteria) {
        StringBuilder url = new StringBuilder(base).append("?q=").append(enc(criteria.query()));
        url.append("&limit=").append(criteria.limit());

        if (criteria.postcode() != null) {
            url.append("&postcode=").append(criteria.postcode());
        }
        if (criteria.type() != null) {
            url.append("&type=").append(criteria.type());
        }
        if (criteria.autocomplete() != null) {
            url.append("&autocomplete=").append(criteria.autocomplete());
        }
        return url.toString();
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}