package fr.exemple.adresses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.function.Executable;

class SearchCriteriaTest {

    private static final String QUERY_AMIENS = "amiens";
    private static final String QUERY_RUE = "rue";
    private static final String VALID_POSTCODE = "80000";
    private static final String INVALID_POSTCODE = "80 00";

    // Vérifie la normalisation de la requête et la limite utilisée par défaut.
    @Test
    void normaliseLaRequeteEtUtiliseLaLimiteParDefaut() {
        // Given
        String query = "  " + QUERY_AMIENS + "  ";

        // When
        SearchCriteria criteria = SearchCriteria.from(query, null, null, null, null);

        // Then
        assertEquals(QUERY_AMIENS, criteria.query());
        assertEquals(5, criteria.limit());
    }

    // Vérifie le rejet des requêtes absentes, vides ou trop courtes.
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "a", "ab", "   " })
    void refuseUneRequeteTropCourte(String query) {
        // Given
        String limit = null;

        // When
        Executable action = () -> SearchCriteria.from(query, limit, null, null, null);

        // Then
        assertThrows(IllegalArgumentException.class, action);
    }

    // Vérifie le rejet d'une requête dépassant la longueur maximale.
    @Test
    void refuseUneRequeteTropLongue() {
        // Given
        String query = "a".repeat(201);

        // When
        Executable action = () -> SearchCriteria.from(query, null, null, null, null);

        // Then
        assertThrows(IllegalArgumentException.class, action);
    }

    // Vérifie l'acceptation d'une requête à la longueur maximale.
    @Test
    void accepteUneRequeteDeDeuxCentsCaracteres() {
        // Given
        String query = "a".repeat(200);

        // When
        SearchCriteria criteria = SearchCriteria.from(query, null, null, null, null);

        // Then
        assertEquals(200, criteria.query().length());
    }

    // Vérifie l'acceptation des limites minimale, maximale et intermédiaire.
    @Test
    void accepteLesLimitesAuxBornesEtEntreLesDeux() {
        // Given
        String query = QUERY_RUE;

        // When
        int normalLimit = SearchCriteria.from(query, "7", null, null, null).limit();
        int minimumLimit = SearchCriteria.from(query, "1", null, null, null).limit();
        int maximumLimit = SearchCriteria.from(query, "50", null, null, null).limit();

        // Then
        assertEquals(7, normalLimit);
        assertEquals(1, minimumLimit);
        assertEquals(50, maximumLimit);
    }

    // Vérifie que la limite par défaut est utilisée quand elle est absente.
    @ParameterizedTest
    @NullAndEmptySource
    void utiliseLaLimiteParDefautSiElleEstAbsente(String limit) {
        // Given
        String query = QUERY_RUE;

        // When
        int parsedLimit = SearchCriteria.from(query, limit, null, null, null).limit();

        // Then
        assertEquals(5, parsedLimit);
    }

    // Vérifie le rejet des limites non numériques ou hors bornes.
    @ParameterizedTest
    @ValueSource(strings = { "abc", "1.5", "99999999999999999999", "0", "-1", "51" })
    void refuseUneLimiteInvalide(String limit) {
        // Given
        String query = QUERY_RUE;

        // When
        Executable action = () -> SearchCriteria.from(query, limit, null, null, null);

        // Then
        assertThrows(IllegalArgumentException.class, action);
    }

    // Vérifie la validation du code postal.
    @Test
    void accepteUnCodePostalValideEtRefuseLesAutres() {
        // Given
        String query = QUERY_RUE;

        // When
        String validPostcode = SearchCriteria.from(query, null, VALID_POSTCODE, null, null).postcode();
        Executable invalidPostcodeAction = () -> SearchCriteria.from(query, null, INVALID_POSTCODE, null, null);

        // Then
        assertEquals(VALID_POSTCODE, validPostcode);
        assertThrows(IllegalArgumentException.class, invalidPostcodeAction);
    }

    // Vérifie l'acceptation de chacun des types d'adresse connus.
    @ParameterizedTest
    @ValueSource(strings = { "housenumber", "street", "locality", "municipality" })
    void accepteLesTypesConnus(String type) {
        // Given
        String query = QUERY_RUE;

        // When
        String acceptedType = SearchCriteria.from(query, null, null, type, null).type();

        // Then
        assertEquals(type, acceptedType);
    }

    // Vérifie le rejet d'un type d'adresse inconnu.
    @Test
    void refuseUnTypeInconnu() {
        // Given
        String query = QUERY_RUE;
        String unknownType = "road";

        // When
        Executable action = () -> SearchCriteria.from(query, null, null, unknownType, null);

        // Then
        assertThrows(IllegalArgumentException.class, action);
    }

    // Vérifie l'acceptation des valeurs connues du paramètre autocomplete.
    @ParameterizedTest
    @ValueSource(strings = { "0", "1" })
    void accepteLesValeursAutocompleteConnues(String autocomplete) {
        // Given
        String query = QUERY_RUE;

        // When
        String acceptedValue = SearchCriteria.from(query, null, null, null, autocomplete).autocomplete();

        // Then
        assertEquals(autocomplete, acceptedValue);
    }

    // Vérifie le rejet d'une valeur autocomplete inconnue.
    @Test
    void refuseUneValeurAutocompleteInconnue() {
        // Given
        String query = QUERY_RUE;
        String unknownAutocomplete = "true";

        // When
        Executable action = () -> SearchCriteria.from(query, null, null, null, unknownAutocomplete);

        // Then
        assertThrows(IllegalArgumentException.class, action);
    }
}