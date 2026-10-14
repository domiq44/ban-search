package fr.exemple.adresses;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Relais vers https://api-adresse.data.gouv.fr/search/ (renvoie le JSON tel
 * quel).
 */
@WebServlet("/api/search")
public class SearchServlet extends HttpServlet {

    private static final String CHARACTER_ENCODING = StandardCharsets.UTF_8.name();
    private static final int JSON_CONTROL_CHARACTER_LIMIT = 0x20;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private final SearchApiClient apiClient;
    private final SearchRateLimiter rateLimiter;

    public SearchServlet() {
        this(new SearchApiClient(), new SearchRateLimiter());
    }

    SearchServlet(String apiUrl) {
        this(new SearchApiClient(apiUrl), new SearchRateLimiter());
    }

    SearchServlet(String apiUrl, SearchRateLimiter rateLimiter) {
        this(new SearchApiClient(apiUrl), rateLimiter);
    }

    SearchServlet(SearchApiClient apiClient, SearchRateLimiter rateLimiter) {
        this.apiClient = apiClient;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding(CHARACTER_ENCODING);
        resp.setContentType("application/json");
        resp.setCharacterEncoding(CHARACTER_ENCODING);

        SearchCriteria criteria;
        try {
            criteria = SearchCriteria.from(
                    req.getParameter("q"),
                    req.getParameter("limit"),
                    req.getParameter("postcode"),
                    req.getParameter("type"),
                    req.getParameter("autocomplete"));
        } catch (IllegalArgumentException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, message(req, e.getMessage()));
            return;
        }

        if (!rateLimiter.allow(req.getRemoteAddr())) {
            writeError(resp, HTTP_TOO_MANY_REQUESTS, message(req, "api.error.rate_limited"));
            return;
        }

        try {
            HttpResponse<String> apiResp = apiClient.search(criteria);
            resp.setStatus(apiResp.statusCode());
            resp.getWriter().write(apiResp.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writeError(resp, HttpServletResponse.SC_BAD_GATEWAY, message(req, "api.error.interrupted"));
        } catch (IOException e) {
            writeError(resp, HttpServletResponse.SC_BAD_GATEWAY, message(req, "api.error.unavailable"));
        }
    }

    private static String message(HttpServletRequest req, String key) {
        String language = req.getParameter("lang");
        Locale locale = switch (language == null ? "" : language) {
            case "en" -> Locale.ENGLISH;
            case "fr" -> Locale.FRENCH;
            default -> req.getLocale();
        };
        if (locale == null) {
            locale = Locale.FRENCH;
        }
        return ResourceBundle.getBundle("fr.exemple.adresses.messages", locale).getString(key);
    }

    private static void writeError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write("{\"error\":\"" + escapeJson(message) + "\"}");
    }

    static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < JSON_CONTROL_CHARACTER_LIMIT) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

}
