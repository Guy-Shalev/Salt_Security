package com.guyshalev.Salt_security.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for TypeValidator.
 * Tests validation of all supported data types with various input formats.
 */
class TypeValidatorTest {

    private TypeValidator typeValidator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        typeValidator = new TypeValidator();
        objectMapper = new ObjectMapper();
    }

    /**
     * Auth-Token Tests
     */
    @Test
    void whenValidatingAuthToken_withValidFormat_thenSuccess() throws Exception {
        JsonNode value = objectMapper.valueToTree("Bearer abc123def456");
        assertTrue(typeValidator.isValidType(value, "Auth-Token"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "bearer abc123",      // wrong case
            "Bearer ",           // missing token
            "BearerNoSpace",     // missing space
            "Bearer abc!@#",     // invalid characters
            "NotABearer"         // wrong format
    })
    void whenValidatingAuthToken_withInvalidFormat_thenFails(String token) throws Exception {
        JsonNode value = objectMapper.valueToTree(token);
        assertFalse(typeValidator.isValidType(value, "Auth-Token"));
    }

    /**
     * UUID Tests
     */
    @Test
    void whenValidatingUUID_withValidFormat_thenSuccess() throws Exception {
        JsonNode value = objectMapper.valueToTree("123e4567-e89b-12d3-a456-426614174000");
        assertTrue(typeValidator.isValidType(value, "UUID"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123e4567-e89b-12d3-a456",             // too short
            "123e4567-e89b-12d3-a456-4266141740g", // invalid character
            "123e4567e89b12d3a456426614174000",    // missing hyphens
            "123e4567-e89b-12d3-a456-42661417400"  // wrong length
    })
    void whenValidatingUUID_withInvalidFormat_thenFails(String uuid) throws Exception {
        JsonNode value = objectMapper.valueToTree(uuid);
        assertFalse(typeValidator.isValidType(value, "UUID"));
    }

    /**
     * Email Tests
     */
    @Test
    void whenValidatingEmail_withValidFormat_thenSuccess() throws Exception {
        JsonNode value = objectMapper.valueToTree("test@example.com");
        assertTrue(typeValidator.isValidType(value, "Email"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid.email",      // missing @
            "@nodomain.com",     // missing local part
            "test@",             // missing domain
            "test@.com",         // missing domain part
            "test@com.",         // trailing dot
            "test@domain..com"   // consecutive dots
    })
    void whenValidatingEmail_withInvalidFormat_thenFails(String email) throws Exception {
        JsonNode value = objectMapper.valueToTree(email);
        assertFalse(typeValidator.isValidType(value, "Email"));
    }

    /**
     * Date Tests
     */
    @Test
    void whenValidatingDate_withValidFormat_thenSuccess() throws Exception {
        JsonNode value = objectMapper.valueToTree("01-01-2024");
        assertTrue(typeValidator.isValidType(value, "Date"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2024-01-01",    // wrong format
            "01/01/2024",    // wrong separator
            "32-01-2024",    // invalid day
            "01-13-2024",    // invalid month
            "01-01-24",      // wrong year format
            "1-1-2024",      // missing leading zeros
            "2024-1-1",      // wrong format
            "not-a-date"     // invalid format
    })
    void whenValidatingDate_withInvalidFormat_thenFails(String date) throws Exception {
        JsonNode value = objectMapper.valueToTree(date);
        assertFalse(typeValidator.isValidType(value, "Date"));
    }

    /**
     * Boolean Tests
     */
    @Test
    void whenValidatingBoolean_withValidValues_thenSuccess() throws Exception {
        // Test boolean node
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree(true), "Boolean"));
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree(false), "Boolean"));

        // Test string representations
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree("true"), "Boolean"));
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree("false"), "Boolean"));
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree("TRUE"), "Boolean"));
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree("FALSE"), "Boolean"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "yes",
            "no",
            "0",
            "1",
            "truee",
            "falsee",
            "not-a-boolean"
    })
    void whenValidatingBoolean_withInvalidValues_thenFails(String value) throws Exception {
        JsonNode jsonNode = objectMapper.valueToTree(value);
        assertFalse(typeValidator.isValidType(jsonNode, "Boolean"));
    }

    /**
     * Integer Tests
     */
    @Test
    void whenValidatingInt_withValidValues_thenSuccess() throws Exception {
        // Test number node
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree(123), "Int"));
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree(-123), "Int"));
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree(0), "Int"));

        // Test string representations
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree("123"), "Int"));
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree("-123"), "Int"));
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree("0"), "Int"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "12.34",        // decimal
            "123a",         // alphanumeric
            "12,345",       // thousand separator
            "not-a-number", // text
            "",            // empty
            " "            // space
    })
    void whenValidatingInt_withInvalidValues_thenFails(String value) throws Exception {
        JsonNode jsonNode = objectMapper.valueToTree(value);
        assertFalse(typeValidator.isValidType(jsonNode, "Int"));
    }

    /**
     * String Tests
     */
    @Test
    void whenValidatingString_withValidValues_thenSuccess() throws Exception {
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree("test"), "String"));
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree(""), "String"));
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree(" "), "String"));
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree("123"), "String"));
    }

    @Test
    void whenValidatingString_withNonStringValues_thenFails() throws Exception {
        assertFalse(typeValidator.isValidType(objectMapper.valueToTree(123), "String"));
        assertFalse(typeValidator.isValidType(objectMapper.valueToTree(true), "String"));
        assertFalse(typeValidator.isValidType(objectMapper.valueToTree(new int[]{1,2,3}), "String"));
    }

    /**
     * List Tests
     */
    @Test
    void whenValidatingList_withValidValues_thenSuccess() throws Exception {
        assertTrue(typeValidator.isValidType(objectMapper.createArrayNode(), "List"));
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree(new int[]{1,2,3}), "List"));
        assertTrue(typeValidator.isValidType(objectMapper.valueToTree(new String[]{"a","b","c"}), "List"));
    }

    @Test
    void whenValidatingList_withNonListValues_thenFails() throws Exception {
        assertFalse(typeValidator.isValidType(objectMapper.valueToTree("not-a-list"), "List"));
        assertFalse(typeValidator.isValidType(objectMapper.valueToTree(123), "List"));
        assertFalse(typeValidator.isValidType(objectMapper.valueToTree(true), "List"));
    }

    /**
     * Invalid Type Tests
     */
    @Test
    void whenValidatingWithInvalidType_thenFails() throws Exception {
        JsonNode value = objectMapper.valueToTree("test");
        assertFalse(typeValidator.isValidType(value, "InvalidType"));
    }

    @Test
    void whenValidatingWithNullValue_thenFails() {
        assertFalse(typeValidator.isValidType(null, "String"));
    }

    @Test
    void whenValidatingWithNullType_thenFails() throws Exception {
        JsonNode value = objectMapper.valueToTree("test");
        assertFalse(typeValidator.isValidType(value, null));
    }
}