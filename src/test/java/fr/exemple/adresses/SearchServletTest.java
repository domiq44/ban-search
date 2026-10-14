package fr.exemple.adresses;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_GATEWAY;
import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static jakarta.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

class SearchServletTest {

    private static final String QUERY_AMIENS = "amiens";
    private static final String EXPECTED_AMIENS_QUERY = "q=" + QUERY_AMIENS + "&limit=5";

    private HttpServer fakeApi;
    private final AtomicReference<String> receivedQuery = new AtomicReference<>();
    private int fakeStatus = SC_OK;
    private String fakeBody = "{\"features\":[]}";

    // Démarre une API HTTP locale simulée pour isoler les tests du service externe.
    @BeforeEach
    void startFakeApi() throws IOException {
        fakeApi = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fakeApi.createContext("/search/", exchange -> {
            receivedQuery.set(exchange.getRequestURI().getRawQuery());
            byte[] body = fakeBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(fakeStatus, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        fakeApi.start();
    }

    // Arrête l'API HTTP locale après chaque test.
    @AfterEach
    void stopFakeApi() {
        fakeApi.stop(0);
    }

    // Retourne l'URL de l'API simulée démarrée pour le test courant.
    private String fakeApiUrl() {
        return "http://127.0.0.1:" + fakeApi.getAddress().getPort() + "/search/";
    }

    // ---- doGet : validation ----

    // Vérifie qu'une requête sans paramètre q est rejetée sans appeler l'API.
    @Test
    void doGetRefuseUnQAbsent() throws Exception {
        // Given
        HttpServletRequest req = request();

        // When
        FakeResponse resp = get(new SearchServlet(fakeApiUrl()), req);

        // Then
        assertEquals(SC_BAD_REQUEST, resp.status);
        assertTrue(resp.body().contains("\"error\""));
        assertEquals(null, receivedQuery.get());
    }

    // Vérifie que les erreurs sont traduites selon la langue explicitement
    // demandée.
    @Test
    void doGetTraduitLesErreursSelonLaLangueDemandee() throws Exception {
        // Given
        HttpServletRequest req = request(Locale.FRENCH, "q", "ab", "lang", "en");

        // When
        FakeResponse resp = get(new SearchServlet(fakeApiUrl()), req);

        // Then
        assertEquals(SC_BAD_REQUEST, resp.status);
        assertTrue(resp.body().contains("The q parameter must contain between 3 and 200 characters"));
    }

    // Vérifie que le choix explicite du français est respecté.
    @Test
    void doGetRespecteLeChoixExpliciteDuFrancais() throws Exception {
        // Given
        HttpServletRequest req = request(Locale.ENGLISH, "q", "ab", "lang", "fr");

        // When
        FakeResponse resp = get(new SearchServlet(fakeApiUrl()), req);

        // Then
        assertEquals(SC_BAD_REQUEST, resp.status);
        assertTrue(resp.body().contains("Le paramètre q doit contenir entre 3 et 200 caractères"));
    }

    // Vérifie que la langue du navigateur est utilisée si le choix est invalide.
    @Test
    void doGetUtiliseLaLangueDuNavigateurSiLeChoixEstInvalide() throws Exception {
        // Given
        HttpServletRequest req = request(Locale.ENGLISH, "q", "ab", "lang", "de");

        // When
        FakeResponse resp = get(new SearchServlet(fakeApiUrl()), req);

        // Then
        assertEquals(SC_BAD_REQUEST, resp.status);
        assertTrue(resp.body().contains("The q parameter must contain between 3 and 200 characters"));
    }

    // Vérifie que le français est utilisé lorsque la locale du navigateur est
    // absente.
    @Test
    void doGetUtiliseLeFrancaisSiLaLocaleNavigateurEstAbsente() throws Exception {
        // Given
        HttpServletRequest req = request((Locale) null, "q", "ab", "lang", "de");

        // When
        FakeResponse resp = get(new SearchServlet(fakeApiUrl()), req);

        // Then
        assertEquals(SC_BAD_REQUEST, resp.status);
        assertTrue(resp.body().contains("Le paramètre q doit contenir entre 3 et 200 caractères"));
    }

    // Vérifie l'échappement des caractères spéciaux dans une chaîne JSON.
    @Test
    void echappeLesCaracteresSpeciauxDansLeJson() {
        // Given
        String value = "quote\" slash\\ backspace\b formfeed\f newline\n carriage\r tab\t" + (char) 1;

        // When
        String escaped = SearchServlet.escapeJson(value);

        // Then
        assertEquals("quote\\\" slash\\\\ backspace\\b formfeed\\f newline\\n carriage\\r tab\\t\\u0001",
                escaped);
    }

    // Vérifie que les requêtes q trop courtes sont rejetées sans appel API.
    @ParameterizedTest
    @ValueSource(strings = { "", "a", "ab", "   ", "  ab  " })
    void doGetRefuseUnQTropCourt(String q) throws Exception {
        // Given
        HttpServletRequest req = request("q", q);

        // When
        FakeResponse resp = get(new SearchServlet(fakeApiUrl()), req);

        // Then
        assertEquals(SC_BAD_REQUEST, resp.status);
        assertEquals(null, receivedQuery.get());
    }

    // Vérifie le rejet d'une requête q dépassant la longueur maximale.
    @Test
    void doGetRefuseUnQTropLong() throws Exception {
        // Given
        HttpServletRequest req = request("q", "a".repeat(201));

        // When
        FakeResponse resp = get(new SearchServlet(fakeApiUrl()), req);

        // Then
        assertEquals(SC_BAD_REQUEST, resp.status);
    }

    // Vérifie qu'un code postal invalide produit une erreur explicite sans appel
    // API.
    @Test
    void doGetExpliqueUnCodePostalInvalideSansAppelerLApi() throws Exception {
        // Given
        HttpServletRequest req = request("q", QUERY_AMIENS, "postcode", "80 00");

        // When
        FakeResponse resp = get(new SearchServlet(fakeApiUrl()), req);

        // Then
        assertEquals(SC_BAD_REQUEST, resp.status);
        assertTrue(resp.body().contains("Le code postal doit contenir exactement 5 chiffres"));
        assertEquals(null, receivedQuery.get());
    }

    // Vérifie la traduction en anglais d'une limite invalide sans appel API.
    @Test
    void doGetExpliqueUneLimiteInvalideEnAnglaisSansAppelerLApi() throws Exception {
        // Given
        HttpServletRequest req = request(Locale.FRENCH, "q", QUERY_AMIENS, "limit", "51", "lang", "en");

        // When
        FakeResponse resp = get(new SearchServlet(fakeApiUrl()), req);

        // Then
        assertEquals(SC_BAD_REQUEST, resp.status);
        assertTrue(resp.body().contains("The limit parameter must be a number between 1 and 50"));
        assertEquals(null, receivedQuery.get());
    }

    // Vérifie qu'un type invalide produit une erreur explicite sans appel API.
    @Test
    void doGetExpliqueUnTypeInvalideSansAppelerLApi() throws Exception {
        // Given
        HttpServletRequest req = request("q", QUERY_AMIENS, "type", "road");

        // When
        FakeResponse resp = get(new SearchServlet(fakeApiUrl()), req);

        // Then
        assertEquals(SC_BAD_REQUEST, resp.status);
        assertTrue(resp.body().contains("Le type d'adresse demandé est invalide"));
        assertEquals(null, receivedQuery.get());
    }

    // Vérifie l'acceptation des requêtes aux longueurs minimale et maximale.
    @Test
    void doGetAccepteUnQDeLongueurLimite() throws Exception {
        // Given
        HttpServletRequest minimumQuery = request("q", "abc");
        HttpServletRequest maximumQuery = request("q", "a".repeat(200));

        // When
        FakeResponse minimumResponse = get(new SearchServlet(fakeApiUrl()), minimumQuery);
        FakeResponse maximumResponse = get(new SearchServlet(fakeApiUrl()), maximumQuery);

        // Then
        assertEquals(SC_OK, minimumResponse.status);
        assertEquals(SC_OK, maximumResponse.status);
    }

    // ---- doGet : relais vers l'API ----

    // Vérifie que le statut, le corps et les métadonnées de l'API sont relayés.
    @Test
    void doGetRelaieLeCorpsEtLeStatutDeLApi() throws Exception {
        // Given
        fakeBody = "{\"features\":[{\"properties\":{\"label\":\"8 Boulevard du Port 80000 Amiens\"}}]}";
        HttpServletRequest req = request("q", "8 bd du port");

        // When
        FakeResponse resp = get(new SearchServlet(fakeApiUrl()), req);

        // Then
        assertEquals(200, resp.status);
        assertEquals(fakeBody, resp.body());
        assertEquals("application/json", resp.contentType);
        assertEquals("UTF-8", resp.encoding);
    }

    // Vérifie la journalisation de la recherche dans les deux formats JSON.
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void doGetJournaliseRechercheStatutDureeEtReponse(boolean prettyJsonLogs) throws Exception {
        // Given
        String query = "adresse-confidentielle";
        fakeBody = "{\"features\":[{\"properties\":{\"label\":\"1 rue du test\"}}]}";
        SearchApiClient apiClient = new SearchApiClient(fakeApiUrl(), prettyJsonLogs);
        SearchServlet servlet = new SearchServlet(apiClient, new SearchRateLimiter());
        Logger logger = Logger.getLogger(SearchApiClient.class.getName());
        Level originalLevel = logger.getLevel();
        ByteArrayOutputStream logOutput = new ByteArrayOutputStream();
        Handler handler = new java.util.logging.StreamHandler(logOutput, new SimpleFormatter());
        logger.setLevel(Level.INFO);
        logger.addHandler(handler);

        try {
            // When
            FakeResponse response = get(servlet, request("q", query));
            handler.flush();
            String log = logOutput.toString(StandardCharsets.UTF_8);

            // Then
            assertEquals(SC_OK, response.status);
            assertTrue(log.contains("query=" + query));
            assertTrue(log.contains("status=200"));
            assertTrue(log.contains("durationMs="));
            if (prettyJsonLogs) {
                assertTrue(log.contains("body={" + System.lineSeparator() + "  \"features\""));
            } else {
                assertTrue(log.contains("body=" + fakeBody));
            }
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(originalLevel);
            handler.close();
        }
    }

    // Vérifie que les paramètres validés sont transmis à l'API.
    @Test
    void doGetTransmetLesParametresALApi() throws Exception {
        // Given
        HttpServletRequest req = request("q", "rue é", "limit", "3", "postcode", "80000");

        // When
        get(new SearchServlet(fakeApiUrl()), req);

        // Then
        assertEquals("q=rue+%C3%A9&limit=3&postcode=80000", receivedQuery.get());
    }

    // Vérifie que les recherches sont limitées par adresse distante.
    @Test
    void doGetLimiteLesRequetesParAdresseDistante() throws Exception {
        // Given
        SearchRateLimiter limiter = new SearchRateLimiter(1, Duration.ofMinutes(1), 10, () -> 0L);
        SearchServlet servlet = new SearchServlet(fakeApiUrl(), limiter);
        HttpServletRequest firstRequest = request("q", "amiens");
        HttpServletRequest secondRequest = request("q", "paris");

        // When
        FakeResponse firstResponse = get(servlet, firstRequest);
        FakeResponse limitedResponse = get(servlet, secondRequest);

        // Then
        assertEquals(SC_OK, firstResponse.status);
        assertEquals(429, limitedResponse.status);
        assertTrue(limitedResponse.body().contains("Trop de recherches"));
        assertEquals("q=amiens&limit=5", receivedQuery.get());
    }

    // Vérifie que le constructeur par défaut utilise l'URL d'API configurée.
    @Test
    void constructeurParDefautUtiliseLaConfigurationDeLApi() throws Exception {
        // Given
        String previousApiUrl = System.getProperty("ban.api.url");
        System.setProperty("ban.api.url", fakeApiUrl());
        try {
            // When
            FakeResponse resp = get(new SearchServlet(), request("q", QUERY_AMIENS));

            // Then
            assertEquals(SC_OK, resp.status);
            assertEquals(EXPECTED_AMIENS_QUERY, receivedQuery.get());
        } finally {
            if (previousApiUrl == null) {
                System.clearProperty("ban.api.url");
            } else {
                System.setProperty("ban.api.url", previousApiUrl);
            }
        }
    }

    // Vérifie que les erreurs renvoyées par l'API sont relayées au client.
    @Test
    void doGetPropageLesErreursDeLApi() throws Exception {
        // Given
        fakeStatus = SC_SERVICE_UNAVAILABLE;
        fakeBody = "{\"message\":\"indisponible\"}";
        HttpServletRequest req = request("q", QUERY_AMIENS);

        // When
        FakeResponse resp = get(new SearchServlet(fakeApiUrl()), req);

        // Then
        assertEquals(SC_SERVICE_UNAVAILABLE, resp.status);
        assertEquals(fakeBody, resp.body());
    }

    // Vérifie que le corps de réponse contenant des accents est relayé en UTF-8.
    @Test
    void doGetRelaieLesAccentsEnUtf8() throws Exception {
        // Given
        fakeBody = "{\"city\":\"Évry-Courcouronnes\"}";
        HttpServletRequest req = request("q", "evry");

        // When
        FakeResponse resp = get(new SearchServlet(fakeApiUrl()), req);

        // Then
        assertEquals(fakeBody, resp.body());
    }

    // Vérifie qu'une API inaccessible produit une réponse 502 explicite.
    @Test
    void doGetRetourne502SiLApiEstInjoignable() throws Exception {
        // Given
        String deadUrl = fakeApiUrl();
        fakeApi.stop(0);
        HttpServletRequest req = request("q", QUERY_AMIENS);

        // When
        FakeResponse resp = get(new SearchServlet(deadUrl), req);

        // Then
        assertEquals(SC_BAD_GATEWAY, resp.status);
        assertTrue(resp.body().contains("injoignable"));
    }

    // Vérifie qu'une interruption produit une réponse 502 et restaure le statut du
    // thread.
    @Test
    void doGetRetourne502EtRestaureLeStatutInterrompu() throws Exception {
        // Given
        Thread.currentThread().interrupt();
        try {
            HttpServletRequest req = request("q", QUERY_AMIENS);

            // When
            FakeResponse resp = get(new SearchServlet(fakeApiUrl()), req);

            // Then
            assertEquals(SC_BAD_GATEWAY, resp.status);
            assertTrue(resp.body().contains("interrompue"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    // ---- utilitaires : requête / réponse factices (sans bibliothèque de mock)
    // ----

    // Crée une requête simulée avec la locale française par défaut.
    private static HttpServletRequest request(String... keyValues) {
        return request(Locale.FRENCH, keyValues);
    }

    // Crée une requête simulée avec la locale et les paramètres spécifiés.
    private static HttpServletRequest request(Locale locale, String... keyValues) {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            params.put(keyValues[i], keyValues[i + 1]);
        }
        return (HttpServletRequest) Proxy.newProxyInstance(
                SearchServletTest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getParameter" -> params.get((String) args[0]);
                    case "getLocale" -> locale;
                    case "getRemoteAddr" -> "127.0.0.1";
                    default -> null;
                });
    }

    // Exécute doGet et retourne la réponse simulée obtenue.
    private static FakeResponse get(SearchServlet servlet, HttpServletRequest req) throws Exception {
        FakeResponse fake = new FakeResponse();
        servlet.doGet(req, fake.proxy());
        return fake;
    }

    private static final class FakeResponse {
        int status = SC_OK;
        String contentType;
        String encoding;
        private final StringWriter out = new StringWriter();

        // Retourne le corps écrit par la servlet dans la réponse simulée.
        String body() {
            return out.toString();
        }

        // Crée le proxy servlet qui capture le statut, les métadonnées et le corps.
        HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    SearchServletTest.class.getClassLoader(),
                    new Class<?>[] { HttpServletResponse.class },
                    (p, method, args) -> switch (method.getName()) {
                        case "setStatus" -> {
                            status = (Integer) args[0];
                            yield null;
                        }
                        case "setContentType" -> {
                            contentType = (String) args[0];
                            yield null;
                        }
                        case "setCharacterEncoding" -> {
                            encoding = (String) args[0];
                            yield null;
                        }
                        case "getWriter" -> new PrintWriter(out, true);
                        default -> null;
                    });
        }
    }
}
