package com.guyshalev.Salt_security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TypeValidatorTest {

    private TypeValidator typeValidator;

    @BeforeEach
    void setUp() {
        typeValidator = new TypeValidator();
    }

    @Nested
    @DisplayName("Auth-Token Tests")
    class AuthTokenTests {
        @Test
        @DisplayName("Valid Auth-Token should pass validation")
        void validAuthToken_ShouldReturnTrue() {
            assertThat(typeValidator.isValidType("Bearer abc123", "Auth-Token")).isTrue();
            assertThat(typeValidator.isValidType("Bearer 123456", "Auth-Token")).isTrue();
            assertThat(typeValidator.isValidType("Bearer abcDEF123", "Auth-Token")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "bearer abc123",      // Wrong case
                "Bearer ",           // No token
                "Bearer abc@123",    // Special character
                "Bearer abc 123",    // Space in token
                "Bearertoken",       // No space
                "Bearer_token",      // Underscore
                "",                  // Empty string
                "Bearer-token",      // Hyphen
                "Token abc123"       // Wrong prefix
        })
        @DisplayName("Invalid Auth-Token formats should fail validation")
        void invalidAuthToken_ShouldReturnFalse(String token) {
            assertThat(typeValidator.isValidType(token, "Auth-Token")).isFalse();
        }

        @Test
        @DisplayName("Non-string Auth-Token should fail validation")
        void nonStringAuthToken_ShouldReturnFalse() {
            assertThat(typeValidator.isValidType(123, "Auth-Token")).isFalse();
            assertThat(typeValidator.isValidType(true, "Auth-Token")).isFalse();
            assertThat(typeValidator.isValidType(null, "Auth-Token")).isFalse();
        }
    }

    @Nested
    @DisplayName("Boolean Tests")
    class BooleanTests {
        @Test
        @DisplayName("Valid boolean values should pass validation")
        void validBoolean_ShouldReturnTrue() {
            assertThat(typeValidator.isValidType(true, "Boolean")).isTrue();
            assertThat(typeValidator.isValidType(false, "Boolean")).isTrue();
            assertThat(typeValidator.isValidType("true", "Boolean")).isTrue();
            assertThat(typeValidator.isValidType("false", "Boolean")).isTrue();
            assertThat(typeValidator.isValidType("FALSE", "Boolean")).isTrue();
            assertThat(typeValidator.isValidType("TRUE", "Boolean")).isTrue();
            assertThat(typeValidator.isValidType("False", "Boolean")).isTrue();
            assertThat(typeValidator.isValidType("True", "Boolean")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "yes",      // Not boolean
                "no",       // Not boolean
                "1",        // Not boolean string
                "0",        // Not boolean string
                "",         // Empty string
                "t",        // Abbreviation
                "f"         // Abbreviation
        })
        @DisplayName("Invalid boolean string values should fail validation")
        void invalidBooleanStrings_ShouldReturnFalse(String value) {
            assertThat(typeValidator.isValidType(value, "Boolean")).isFalse();
        }

        @Test
        @DisplayName("Non-boolean values should fail validation")
        void nonBooleanValues_ShouldReturnFalse() {
            assertThat(typeValidator.isValidType(1, "Boolean")).isFalse();
            assertThat(typeValidator.isValidType(0, "Boolean")).isFalse();
            assertThat(typeValidator.isValidType(null, "Boolean")).isFalse();
            assertThat(typeValidator.isValidType(new Object(), "Boolean")).isFalse();
        }
    }

    @Nested
    @DisplayName("Int Tests")
    class IntTests {
        @Test
        @DisplayName("Valid integer values should pass validation")
        void validInt_ShouldReturnTrue() {
            assertThat(typeValidator.isValidType(42, "Int")).isTrue();
            assertThat(typeValidator.isValidType(-42, "Int")).isTrue();
            assertThat(typeValidator.isValidType(0, "Int")).isTrue();
            assertThat(typeValidator.isValidType("42", "Int")).isTrue();
            assertThat(typeValidator.isValidType("-42", "Int")).isTrue();
            assertThat(typeValidator.isValidType("0", "Int")).isTrue();
            assertThat(typeValidator.isValidType(Integer.MAX_VALUE, "Int")).isTrue();
            assertThat(typeValidator.isValidType(Integer.MIN_VALUE, "Int")).isTrue();
            assertThat(typeValidator.isValidType(42L, "Int")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "42.0",        // Decimal
                "4e2",         // Scientific notation
                "42,000",      // Thousands separator
                " 42",         // Leading space
                "42 ",         // Trailing space
                "abc",         // Letters
                ""           // Empty string
        })
        @DisplayName("Invalid integer string formats should fail validation")
        void invalidIntStrings_ShouldReturnFalse(String value) {
            assertThat(typeValidator.isValidType(value, "Int")).isFalse();
        }

        @Test
        @DisplayName("Non-integer values should fail validation")
        void nonIntegerValues_ShouldReturnFalse() {
            assertThat(typeValidator.isValidType(42.0, "Int")).isFalse();
            assertThat(typeValidator.isValidType(true, "Int")).isFalse();
            assertThat(typeValidator.isValidType(null, "Int")).isFalse();
        }
    }

    @Nested
    @DisplayName("String Tests")
    class StringTests {
        @Test
        @DisplayName("Valid string values should pass validation")
        void validString_ShouldReturnTrue() {
            assertThat(typeValidator.isValidType("test", "String")).isTrue();
            assertThat(typeValidator.isValidType("", "String")).isTrue();
            assertThat(typeValidator.isValidType(" ", "String")).isTrue();
            assertThat(typeValidator.isValidType("123", "String")).isTrue();
            assertThat(typeValidator.isValidType("null", "String")).isTrue();
        }

        @Test
        @DisplayName("Non-string values should fail validation")
        void nonStringValues_ShouldReturnFalse() {
            assertThat(typeValidator.isValidType(123, "String")).isFalse();
            assertThat(typeValidator.isValidType(true, "String")).isFalse();
            assertThat(typeValidator.isValidType(null, "String")).isFalse();
            assertThat(typeValidator.isValidType(new Object(), "String")).isFalse();
        }
    }

    @Nested
    @DisplayName("List Tests")
    class ListTests {
        @Test
        @DisplayName("Valid list values should pass validation")
        void validList_ShouldReturnTrue() {
            assertThat(typeValidator.isValidType(Collections.emptyList(), "List")).isTrue();
            assertThat(typeValidator.isValidType(Arrays.asList(1, 2, 3), "List")).isTrue();
            assertThat(typeValidator.isValidType(Arrays.asList("a", "b", "c"), "List")).isTrue();
            assertThat(typeValidator.isValidType(Collections.singletonList(1), "List")).isTrue();
        }

        @Test
        @DisplayName("Array values should fail List validation")
        void arrayValues_ShouldReturnFalse() {
            assertThat(typeValidator.isValidType(new int[]{1, 2, 3}, "List")).isFalse();
            assertThat(typeValidator.isValidType(new String[]{"a", "b", "c"}, "List")).isFalse();
        }

        @Test
        @DisplayName("Non-list values should fail validation")
        void nonListValues_ShouldReturnFalse() {
            assertThat(typeValidator.isValidType("[]", "List")).isFalse();
            assertThat(typeValidator.isValidType(123, "List")).isFalse();
            assertThat(typeValidator.isValidType(null, "List")).isFalse();
            assertThat(typeValidator.isValidType(new Object(), "List")).isFalse();
        }
    }

    @Nested
    @DisplayName("Date Tests")
    class DateTests {
        @Test
        @DisplayName("Valid date formats should pass validation")
        void validDate_ShouldReturnTrue() {
            assertThat(typeValidator.isValidType("01-01-2024", "Date")).isTrue();
            assertThat(typeValidator.isValidType("31-12-2023", "Date")).isTrue();
            assertThat(typeValidator.isValidType("15-06-2024", "Date")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "2024-01-01",    // Wrong format
                "01/01/2024",    // Wrong separator
                "1-1-2024",      // Missing leading zeros
                "00-00-2024",    // Invalid day/month
                "32-13-2024",    // Invalid day/month
                "01-01-24",      // Two-digit year
                "2024-1",        // Incomplete
                "",              // Empty string
                "abc",           // Invalid format
                "01-01-202A"     // Non-numeric year
        })
        @DisplayName("Invalid date formats should fail validation")
        void invalidDate_ShouldReturnFalse(String date) {
            assertThat(typeValidator.isValidType(date, "Date")).isFalse();
        }

        @Test
        @DisplayName("Non-string dates should fail validation")
        void nonStringDates_ShouldReturnFalse() {
            assertThat(typeValidator.isValidType(123, "Date")).isFalse();
            assertThat(typeValidator.isValidType(true, "Date")).isFalse();
            assertThat(typeValidator.isValidType(null, "Date")).isFalse();
        }
    }

    @Nested
    @DisplayName("Email Tests")
    class EmailTests {
        @Test
        @DisplayName("Valid email formats should pass validation")
        void validEmail_ShouldReturnTrue() {
            assertThat(typeValidator.isValidType("test@example.com", "Email")).isTrue();
            assertThat(typeValidator.isValidType("test.name@example.com", "Email")).isTrue();
            assertThat(typeValidator.isValidType("test@sub.example.com", "Email")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "@example.com",      // No local part
                "test@",             // No domain
                "test@.com",         // Domain starts with dot
                "test@example.",     // Domain ends with dot
                "test@example..com", // Consecutive dots in domain
                "test@exam ple.com", // Space in domain
                "tes t@example.com", // Space in local part
                "",                  // Empty string
                "test.example.com",  // Missing @
                "@"                  // Just @
        })
        @DisplayName("Invalid email formats should fail validation")
        void invalidEmail_ShouldReturnFalse(String email) {
            assertThat(typeValidator.isValidType(email, "Email")).isFalse();
        }

        @Test
        @DisplayName("Non-string emails should fail validation")
        void nonStringEmails_ShouldReturnFalse() {
            assertThat(typeValidator.isValidType(123, "Email")).isFalse();
            assertThat(typeValidator.isValidType(true, "Email")).isFalse();
            assertThat(typeValidator.isValidType(null, "Email")).isFalse();
        }
    }

    @Nested
    @DisplayName("UUID Tests")
    class UUIDTests {
        @Test
        @DisplayName("Valid UUID formats should pass validation")
        void validUUID_ShouldReturnTrue() {
            assertThat(typeValidator.isValidType("123e4567-e89b-12d3-a456-426614174000", "UUID")).isTrue();
            assertThat(typeValidator.isValidType("00000000-0000-0000-0000-000000000000", "UUID")).isTrue();
            assertThat(typeValidator.isValidType("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF", "UUID")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "123e4567e89b12d3a456426614174000",     // No hyphens
                "123e4567-e89b-12d3-a456-42661417400",  // Too short
                "123e4567-e89b-12d3-a456-4266141740000", // Too long
                "123e4567-e89b-12d3-a456-42661417400g", // Invalid character
                "123e4567-e89b-12d3-a456",              // Incomplete
                "",                                      // Empty string
                "123e4567 e89b 12d3 a456 426614174000", // Spaces instead of hyphens
                "123e4567#e89b#12d3#a456#426614174000"  // Wrong separator
        })
        @DisplayName("Invalid UUID formats should fail validation")
        void invalidUUID_ShouldReturnFalse(String uuid) {
            assertThat(typeValidator.isValidType(uuid, "UUID")).isFalse();
        }

        @Test
        @DisplayName("Non-string UUIDs should fail validation")
        void nonStringUUIDs_ShouldReturnFalse() {
            assertThat(typeValidator.isValidType(123, "UUID")).isFalse();
            assertThat(typeValidator.isValidType(true, "UUID")).isFalse();
            assertThat(typeValidator.isValidType(null, "UUID")).isFalse();
        }
    }

    @Nested
    @DisplayName("Invalid Type Tests")
    class InvalidTypeTests {
        @Test
        @DisplayName("Unknown types should fail validation")
        void unknownType_ShouldReturnFalse() {
            assertThat(typeValidator.isValidType("test", "UnknownType")).isFalse();
            assertThat(typeValidator.isValidType(123, "CustomType")).isFalse();
            assertThat(typeValidator.isValidType(true, "NewType")).isFalse();
        }

        @Test
        @DisplayName("Null type should fail validation")
        void nullType_ShouldReturnFalse() {
            assertThat(typeValidator.isValidType("test", null)).isFalse();
            assertThat(typeValidator.isValidType(123, null)).isFalse();
            assertThat(typeValidator.isValidType(null, null)).isFalse();
        }

        @Test
        @DisplayName("Empty type should fail validation")
        void emptyType_ShouldReturnFalse() {
            assertThat(typeValidator.isValidType("test", "")).isFalse();
            assertThat(typeValidator.isValidType(123, "")).isFalse();
            assertThat(typeValidator.isValidType(null, "")).isFalse();
        }
    }
}