package com.guyshalev.Salt_security.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.guyshalev.Salt_security.model.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }


    @Nested
    @DisplayName("Handle Malformed JSON")
    class HandleMalformedJson {

        @Test
        @DisplayName("Should handle JSON with missing comma")
        void handleMissingComma() {
            // Arrange
            JsonParseException parseException = new JsonParseException(null,
                    "Unexpected character ('\"' (code 34)): was expecting comma to separate Object entries");
            HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                    "Failed to read JSON", parseException, null);

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpMessageNotReadable(ex);

            // Assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("Malformed JSON");
            assertThat(response.getBody().getDetails())
                    .containsKey("error")
                    .containsValue("Invalid JSON syntax: Missing comma or incorrect JSON syntax");
        }

        @Test
        @DisplayName("Should handle completely invalid JSON")
        void handleInvalidJson() {
            // Arrange
            JsonParseException parseException = new JsonParseException(null,
                    "Unrecognized token 'This': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')");
            HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                    "Failed to read JSON", parseException, null);

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpMessageNotReadable(ex);

            // Assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("Malformed JSON");
            assertThat(response.getBody().getDetails())
                    .containsKey("error");
        }
    }

    @Nested
    @DisplayName("Handle Invalid Request Structure")
    class HandleInvalidStructure {

        @Test
        @DisplayName("Should handle wrong type for array")
        void handleWrongTypeForArray() {
            // Arrange
            JsonMappingException mappingException = mock(JsonMappingException.class);
            JsonMappingException.Reference ref = new JsonMappingException.Reference(null, "query_params");
            when(mappingException.getPath()).thenReturn(java.util.Collections.singletonList(ref));
            when(mappingException.getMessage())
                    .thenReturn("Cannot deserialize value of type `java.util.ArrayList` from Object value");

            HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                    "Failed to read JSON", mappingException, null);

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpMessageNotReadable(ex);

            // Assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("Invalid request structure");
            assertThat(response.getBody().getDetails())
                    .containsEntry("field", "query_params")
                    .containsKey("error");
        }

        @Test
        @DisplayName("Should handle missing field reference")
        void handleMissingFieldReference() {
            // Arrange
            JsonMappingException mappingException = mock(JsonMappingException.class);
            when(mappingException.getPath()).thenReturn(java.util.Collections.emptyList());
            when(mappingException.getMessage())
                    .thenReturn("Missing required field");

            HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                    "Failed to read JSON", mappingException, null);

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpMessageNotReadable(ex);

            // Assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("Invalid request structure");
            assertThat(response.getBody().getDetails())
                    .containsEntry("field", "request body");
        }
    }

    @Nested
    @DisplayName("Handle Unexpected Errors")
    class HandleUnexpectedErrors {

        @Test
        @DisplayName("Should handle generic runtime exception")
        void handleRuntimeException() {
            // Arrange
            RuntimeException ex = new RuntimeException("Something went wrong");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleException(ex);

            // Assert
            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
            assertThat(response.getBody().getDetails())
                    .containsEntry("error", "Something went wrong");
        }

        @Test
        @DisplayName("Should handle null pointer exception")
        void handleNullPointerException() {
            // Arrange
            NullPointerException ex = new NullPointerException("Null value encountered");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleException(ex);

            // Assert
            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
            assertThat(response.getBody().getDetails())
                    .containsEntry("error", "Null value encountered");
        }

        @Test
        @DisplayName("Should handle exception without message")
        void handleExceptionWithoutMessage() {
            // Arrange
            Exception ex = new Exception();

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleException(ex);

            // Assert
            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
            assertThat(response.getBody().getDetails())
                    .containsEntry("error", "No additional error details available");
        }
    }
}