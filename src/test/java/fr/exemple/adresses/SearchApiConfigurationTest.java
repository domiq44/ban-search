package fr.exemple.adresses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

class SearchApiConfigurationTest {

    private static final String DEFAULT_API_URL = "https://api-adresse.data.gouv.fr/search/";
    private static final String SYSTEM_API_URL = "https://system.example/search/";
    private static final String ENVIRONMENT_API_URL = "https://environment.example/search/";
    private static final String HTTP_API_URL = "http://localhost/search/";
    private static final String NON_HTTP_API_URL = "ftp://api.example/search/";
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 5;
    private static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 10;

    // Vérifie la lecture des propriétés et des délais de configuration.
    @Test
    void interpreteLesValeursDesProprietesFournies() {
        // Given
        Properties properties = defaults();

        // When
        SearchApiConfiguration configuration = SearchApiConfiguration.from(properties, Map.of(), new Properties());

        // Then
        assertEquals(DEFAULT_API_URL, configuration.apiUrl());
        assertEquals(Duration.ofSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS), configuration.connectTimeout());
        assertEquals(Duration.ofSeconds(DEFAULT_REQUEST_TIMEOUT_SECONDS), configuration.requestTimeout());
    }

    // Vérifie que la configuration est chargée depuis le classpath.
    @Test
    void chargeUneConfigurationValideDepuisLeClasspath() {
        // Given
        String httpScheme = "http://";
        String httpsScheme = "https://";

        // When
        SearchApiConfiguration configuration = SearchApiConfiguration.load();

        // Then
        assertTrue(configuration.apiUrl().startsWith(httpScheme) || configuration.apiUrl().startsWith(httpsScheme));
        assertTrue(configuration.connectTimeout().toSeconds() > 0);
        assertTrue(configuration.requestTimeout().toSeconds() > 0);
    }

    // Vérifie la priorité des propriétés JVM sur l'environnement et les valeurs par
    // défaut.
    @Test
    void lesProprietesJvmPrennentLePasSurEnvironnementEtValeursParDefaut() {
        // Given
        Properties systemProperties = new Properties();
        systemProperties.setProperty("ban.api.url", SYSTEM_API_URL);

        // When
        SearchApiConfiguration configuration = SearchApiConfiguration.from(
                defaults(),
                Map.of("BAN_API_URL", ENVIRONMENT_API_URL),
                systemProperties);

        // Then
        assertEquals(SYSTEM_API_URL, configuration.apiUrl());
    }

    // Vérifie qu'une variable d'environnement remplace la valeur par défaut.
    @Test
    void lesVariablesEnvironnementPrennentLePasSurLesValeursParDefaut() {
        // Given
        Map<String, String> environment = Map.of("BAN_API_REQUEST_TIMEOUT_SECONDS", "15");

        // When
        SearchApiConfiguration configuration = SearchApiConfiguration.from(
                defaults(),
                environment,
                new Properties());

        // Then
        assertEquals(Duration.ofSeconds(15), configuration.requestTimeout());
    }

    // Vérifie le rejet d'un délai nul dans les propriétés.
    @Test
    void refuseUnDelaiNul() {
        // Given
        Properties properties = defaults();
        properties.setProperty("ban.api.connect-timeout-seconds", "0");

        // When
        Executable action = () -> SearchApiConfiguration.from(properties, Map.of(), new Properties());

        // Then
        assertThrows(IllegalStateException.class, action);
    }

    // Vérifie le rejet d'un délai qui n'est pas numérique.
    @Test
    void refuseUnDelaiNonNumerique() {
        // Given
        Properties properties = defaults();
        properties.setProperty("ban.api.request-timeout-seconds", "fast");

        // When
        Executable action = () -> SearchApiConfiguration.from(properties, Map.of(), new Properties());

        // Then
        assertThrows(IllegalStateException.class, action);
    }

    // Vérifie le rejet d'une URL utilisant un protocole autre que HTTP(S).
    @Test
    void refuseUneUrlQuiNUtilisePasHttp() {
        // Given
        Properties properties = defaults();
        properties.setProperty("ban.api.url", NON_HTTP_API_URL);

        // When
        Executable action = () -> SearchApiConfiguration.from(properties, Map.of(), new Properties());

        // Then
        assertThrows(IllegalArgumentException.class, action);
    }

    // Vérifie le rejet d'une URL HTTP sans nom d'hôte.
    @Test
    void refuseUneUrlHttpSansHote() {
        // Given
        Properties properties = defaults();
        properties.setProperty("ban.api.url", "http:api.example/search/");

        // When
        Executable action = () -> SearchApiConfiguration.from(properties, Map.of(), new Properties());

        // Then
        assertThrows(IllegalArgumentException.class, action);
    }

    // Vérifie l'acceptation d'une URL HTTP avec des délais positifs.
    @Test
    void accepteUneUrlHttpAvecDesDelaisPositifs() {
        // Given
        Duration connectTimeout = Duration.ofSeconds(1);
        Duration requestTimeout = Duration.ofSeconds(2);

        // When
        SearchApiConfiguration configuration = new SearchApiConfiguration(
                HTTP_API_URL, connectTimeout, requestTimeout);

        // Then
        assertEquals(HTTP_API_URL, configuration.apiUrl());
        assertEquals(connectTimeout, configuration.connectTimeout());
        assertEquals(requestTimeout, configuration.requestTimeout());
    }

    // Vérifie que le constructeur rejette un délai nul.
    @Test
    void refuseUnDelaiNulAuConstructeur() {
        // Given
        String apiUrl = "https://api.example/";
        Duration connectTimeout = Duration.ZERO;
        Duration requestTimeout = Duration.ofSeconds(1);

        // When
        Executable action = () -> new SearchApiConfiguration(apiUrl, connectTimeout, requestTimeout);

        // Then
        assertThrows(IllegalArgumentException.class, action);
    }

    // Vérifie que le constructeur rejette un délai négatif.
    @Test
    void refuseUnDelaiNegatifAuConstructeur() {
        // Given
        String apiUrl = "https://api.example/";
        Duration connectTimeout = Duration.ofSeconds(1);
        Duration requestTimeout = Duration.ofSeconds(-1);

        // When
        Executable action = () -> new SearchApiConfiguration(apiUrl, connectTimeout, requestTimeout);

        // Then
        assertThrows(IllegalArgumentException.class, action);
    }

    // Vérifie le rejet d'une propriété obligatoire manquante.
    @Test
    void refuseUneProprieteDeConfigurationManquante() {
        // Given
        Properties properties = defaults();
        properties.remove("ban.api.request-timeout-seconds");

        // When
        Executable action = () -> SearchApiConfiguration.from(properties, Map.of(), new Properties());

        // Then
        assertThrows(IllegalStateException.class, action);
    }

    // Prépare les propriétés de configuration valides utilisées par les tests.
    private static Properties defaults() {
        Properties properties = new Properties();
        properties.setProperty("ban.api.url", DEFAULT_API_URL);
        properties.setProperty("ban.api.connect-timeout-seconds", Integer.toString(DEFAULT_CONNECT_TIMEOUT_SECONDS));
        properties.setProperty("ban.api.request-timeout-seconds", Integer.toString(DEFAULT_REQUEST_TIMEOUT_SECONDS));
        return properties;
    }
}