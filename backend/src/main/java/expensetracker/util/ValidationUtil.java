package expensetracker.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class ValidationUtil {

    private static final Pattern HTML_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /**
     * Sanitizes a string by removing HTML tags and trimming whitespace.
     *
     * @param str the input string
     * @param maxLength the maximum allowed length after sanitization
     * @return the sanitized string
     * @throws IllegalArgumentException if the string exceeds maxLength or is empty after sanitization
     */
    public static String sanitizeString(String str, int maxLength) {
        if (str == null) {
            throw new IllegalArgumentException("String cannot be null");
        }

        // Remove HTML tags
        String sanitized = HTML_PATTERN.matcher(str).replaceAll("");

        // Trim whitespace
        sanitized = sanitized.trim();

        // Check if empty after sanitization
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("String cannot be empty");
        }

        // Check length
        if (sanitized.length() > maxLength) {
            throw new IllegalArgumentException("String exceeds maximum length of " + maxLength + " characters");
        }

        return sanitized;
    }

    /**
     * Validates that a required field is not null or blank.
     *
     * @param value the field value
     * @param fieldName the name of the field
     * @throws IllegalArgumentException if the field is null, blank, or invalid
     */
    public static String validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * Validates that an amount is a positive number.
     *
     * @param amount the amount to validate
     * @throws IllegalArgumentException if the amount is null, not a valid number, or not positive
     */
    public static BigDecimal validateAmount(Object amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }

        try {
            BigDecimal bd = new BigDecimal(amount.toString());
            if (bd.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be a positive number");
            }
            return bd;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a valid number");
        }
    }

    /**
     * Validates that a date string is in the format yyyy-MM-dd and is a valid date.
     *
     * @param dateStr the date string to validate
     * @throws IllegalArgumentException if the date is invalid or in wrong format
     */
    public static LocalDate validateDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("Date is required");
        }

        try {
            LocalDate date = LocalDate.parse(dateStr);
            if (date.isAfter(LocalDate.now().plusDays(1))) {
                throw new IllegalArgumentException("Date cannot be in the future");
            }
            return date;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date must be in yyyy-MM-dd format");
        }
    }

    /**
     * Validates an email address format.
     *
     * @param email the email to validate
     * @throws IllegalArgumentException if the email format is invalid
     */
    public static String validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        String trimmed = email.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Email format is invalid");
        }

        return trimmed;
    }

    /**
     * Validates that a category is one of the valid values.
     *
     * @param category the category to validate
     * @param validCategories the array of valid category values
     * @throws IllegalArgumentException if the category is not in the valid list
     */
    public static String validateCategory(String category, String[] validCategories) {
        String validated = validateRequired(category, "Category");

        for (String valid : validCategories) {
            if (valid.equals(validated)) {
                return validated;
            }
        }

        throw new IllegalArgumentException("Category must be one of: " + String.join(", ", validCategories));
    }

    /**
     * Validates currency is either INR or USD.
     *
     * @param currency the currency code to validate
     * @throws IllegalArgumentException if currency is invalid
     */
    public static String validateCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "INR"; // default
        }

        String upper = currency.trim().toUpperCase();
        if (!upper.equals("INR") && !upper.equals("USD")) {
            throw new IllegalArgumentException("Currency must be INR or USD");
        }

        return upper;
    }

    // ── Convenience boolean/sanitize helpers (used by tests and callers) ──

    /**
     * Sanitizes a string by stripping HTML tags, trimming, and truncating
     * to a default max length of 500 characters. Returns "" for null input.
     */
    public static String sanitize(String input) {
        return sanitize(input, 500);
    }

    /**
     * Sanitizes a string by stripping HTML tags, trimming, and truncating
     * to the given maxLength. Returns "" for null input.
     */
    public static String sanitize(String input, int maxLength) {
        if (input == null) return "";
        String cleaned = HTML_PATTERN.matcher(input).replaceAll("").trim();
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }
        return cleaned;
    }

    /**
     * Returns true if the email has a valid format, false otherwise (including null).
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Returns true if the amount is a positive number, false otherwise.
     */
    public static boolean isValidAmount(double amount) {
        return amount > 0;
    }

    /**
     * Returns true if the string is a valid yyyy-MM-dd date, false otherwise (including null).
     */
    public static boolean isValidDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return false;
        try {
            java.time.LocalDate.parse(dateStr);
            return true;
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }
}
