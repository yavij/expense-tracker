package expensetracker.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidationUtil Tests")
class ValidationUtilTest {

    @Test
    @DisplayName("sanitize should strip HTML tags")
    void sanitize_stripsHtmlTags() {
        assertEquals("hello world", ValidationUtil.sanitize("<b>hello</b> <script>alert('xss')</script>world"));
    }

    @Test
    @DisplayName("sanitize should return empty string for null")
    void sanitize_returnsEmptyForNull() {
        assertEquals("", ValidationUtil.sanitize(null));
    }

    @Test
    @DisplayName("sanitize should trim whitespace")
    void sanitize_trimsWhitespace() {
        assertEquals("hello", ValidationUtil.sanitize("  hello  "));
    }

    @Test
    @DisplayName("sanitize should enforce max length")
    void sanitize_enforcesMaxLength() {
        String longString = "a".repeat(600);
        String result = ValidationUtil.sanitize(longString, 500);
        assertEquals(500, result.length());
    }

    @Test
    @DisplayName("sanitize with default max length should allow up to 500 chars")
    void sanitize_defaultMaxLength() {
        String input = "a".repeat(501);
        assertEquals(500, ValidationUtil.sanitize(input).length());
    }

    @ParameterizedTest
    @ValueSource(strings = {"test@example.com", "user.name+tag@domain.co.in", "a@b.c"})
    @DisplayName("isValidEmail should accept valid emails")
    void isValidEmail_validEmails(String email) {
        assertTrue(ValidationUtil.isValidEmail(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "@no-local.com", "spaces in@email.com", ""})
    @DisplayName("isValidEmail should reject invalid emails")
    void isValidEmail_invalidEmails(String email) {
        assertFalse(ValidationUtil.isValidEmail(email));
    }

    @Test
    @DisplayName("isValidEmail should reject null")
    void isValidEmail_rejectsNull() {
        assertFalse(ValidationUtil.isValidEmail(null));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.01, 1.0, 99999999.99})
    @DisplayName("isValidAmount should accept positive amounts")
    void isValidAmount_positiveAmounts(double amount) {
        assertTrue(ValidationUtil.isValidAmount(amount));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0, -0.01})
    @DisplayName("isValidAmount should reject zero and negative amounts")
    void isValidAmount_rejectsZeroAndNegative(double amount) {
        assertFalse(ValidationUtil.isValidAmount(amount));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-03-15", "2025-01-01", "2030-12-31"})
    @DisplayName("isValidDate should accept valid dates")
    void isValidDate_validDates(String date) {
        assertTrue(ValidationUtil.isValidDate(date));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-date", "03-15-2026", "2026/03/15", "2026-13-01", ""})
    @DisplayName("isValidDate should reject invalid dates")
    void isValidDate_invalidDates(String date) {
        assertFalse(ValidationUtil.isValidDate(date));
    }

    @Test
    @DisplayName("isValidDate should reject null")
    void isValidDate_rejectsNull() {
        assertFalse(ValidationUtil.isValidDate(null));
    }
}
