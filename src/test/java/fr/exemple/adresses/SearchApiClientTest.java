package fr.exemple.adresses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SearchApiClientTest {

    private static final String BASE = "http://api.test/search/";
    private static final String QUERY_AMIENS = "amiens";
    private static final String QUERY_RUE = "rue";
    private static final String QUERY_AMIENS_WITH_SPACES = "  " + QUERY_AMIENS + "  ";
    private static final String VALID_LIMIT = "10";
    private static final String VALID_POSTCODE = "80000";
    private static final String VALID_TYPE = "street";
    private static final String AUTOCOMPLETE_ENABLED = "0";

    // Vérifie la génération d'une URL minimale avec la limite par défaut.
    @Test
    void urlMinimale_utiliseLimiteParDefaut() {
        // Given
        SearchCriteria criteria = criteria(QUERY_AMIENS);

        // When
        String url = SearchApiClient.buildApiUrl(BASE, criteria);

        // Then
        assertEquals(BASE + "?q=" + QUERY_AMIENS + "&limit=5", url);
    }

    // Vérifie l'encodage des espaces et des caractères accentués dans la requête.
    @Test
    void urlEncodeEspacesEtAccents() {
        // Given
        SearchCriteria criteria = criteria("8 bd du port é");

        // When
        String url = SearchApiClient.buildApiUrl(BASE, criteria);

        // Then
        assertEquals(BASE + "?q=8+bd+du+port+%C3%A9&limit=5", url);
    }

    // Vérifie que les caractères pouvant modifier l'URL sont encodés.
    @Test
    void urlEncodeLesCaracteresDInjection() {
        // Given
        SearchCriteria criteria = criteria("a&limit=999#x");

        // When
        String url = SearchApiClient.buildApiUrl(BASE, criteria);

        // Then
        assertEquals(BASE + "?q=a%26limit%3D999%23x&limit=5", url);
    }

    // Vérifie que les espaces autour du terme recherché sont supprimés.
    @Test
    void urlSupprimeLesEspacesAutourDeQ() {
        // Given
        SearchCriteria criteria = criteria(QUERY_AMIENS_WITH_SPACES);

        // When
        String url = SearchApiClient.buildApiUrl(BASE, criteria);

        // Then
        assertEquals(BASE + "?q=" + QUERY_AMIENS + "&limit=5", url);
    }

    // Vérifie que tous les paramètres valides sont ajoutés à l'URL.
    @Test
    void urlAccepteTousLesParametresValides() {
        // Given
        SearchCriteria criteria = criteria(QUERY_RUE, VALID_LIMIT, VALID_POSTCODE, VALID_TYPE, AUTOCOMPLETE_ENABLED);

        // When
        String url = SearchApiClient.buildApiUrl(BASE, criteria);

        // Then
        assertEquals(BASE + "?q=" + QUERY_RUE + "&limit=" + VALID_LIMIT
                + "&postcode=" + VALID_POSTCODE + "&type=" + VALID_TYPE
                + "&autocomplete=" + AUTOCOMPLETE_ENABLED, url);
    }

    // Construit des critères avec les seuls paramètres nécessaires au test.
    private static SearchCriteria criteria(String query) {
        return criteria(query, null, null, null, null);
    }

    // Construit des critères à partir de l'ensemble des paramètres fournis.
    private static SearchCriteria criteria(String query, String limit, String postcode, String type,
            String autocomplete) {
        return SearchCriteria.from(query, limit, postcode, type, autocomplete);
    }
}