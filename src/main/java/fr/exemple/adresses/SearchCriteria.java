package fr.exemple.adresses;

import java.util.Set;
import java.util.regex.Pattern;

record SearchCriteria(String query, int limit, String postcode, String type, String autocomplete) {

    private static final int MIN_QUERY_LENGTH = 3;
    private static final int MAX_QUERY_LENGTH = 200;
    private static final int DEFAULT_LIMIT = 5;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 50;
    private static final int POSTCODE_DIGITS = 5;
    private static final Pattern POSTCODE_PATTERN = Pattern.compile("\\d{" + POSTCODE_DIGITS + "}");
    private static final Set<String> VALID_TYPES = Set.of("housenumber", "street", "locality", "municipality");
    private static final Set<String> VALID_AUTOCOMPLETE_VALUES = Set.of("0", "1");

    static SearchCriteria from(String query, String limit, String postcode, String type, String autocomplete) {
        if (query == null || query.trim().length() < MIN_QUERY_LENGTH || query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("api.error.invalid_query");
        }

        return new SearchCriteria(
                query.trim(),
                parseLimit(limit),
                validPostcode(postcode),
                validChoice(type, VALID_TYPES, "api.error.invalid_type"),
                validChoice(autocomplete, VALID_AUTOCOMPLETE_VALUES, "api.error.invalid_autocomplete"));
    }

    private static int parseLimit(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_LIMIT;
        }
        try {
            int limit = Integer.parseInt(value);
            if (limit < MIN_LIMIT || limit > MAX_LIMIT) {
                throw new IllegalArgumentException("api.error.invalid_limit");
            }
            return limit;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("api.error.invalid_limit", e);
        }
    }

    private static String validPostcode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!POSTCODE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("api.error.invalid_postcode");
        }
        return value;
    }

    private static String validChoice(String value, Set<String> choices, String errorKey) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!choices.contains(value)) {
            throw new IllegalArgumentException(errorKey);
        }
        return value;
    }
}