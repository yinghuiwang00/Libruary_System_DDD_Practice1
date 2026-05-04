package com.library.catalog.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse tests")
class ApiResponseTest {

    // ---------------------------------------------------------------
    // ApiResponse.ok() factory
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("ApiResponse.ok() factory")
    class OkFactoryTests {

        @Test
        @DisplayName("should create success response with data")
        void shouldCreateSuccessResponseWithData() {
            ApiResponse<String> response = ApiResponse.ok("test data");

            assertThat(response.success()).isTrue();
            assertThat(response.data()).isEqualTo("test data");
        }

        @Test
        @DisplayName("should set error and errorCode to null for success response")
        void shouldSetNullErrorsForSuccessResponse() {
            ApiResponse<String> response = ApiResponse.ok("data");

            assertThat(response.error()).isNull();
            assertThat(response.errorCode()).isNull();
        }

        @Test
        @DisplayName("should handle null data in success response")
        void shouldHandleNullDataInSuccessResponse() {
            ApiResponse<String> response = ApiResponse.ok(null);

            assertThat(response.success()).isTrue();
            assertThat(response.data()).isNull();
            assertThat(response.error()).isNull();
            assertThat(response.errorCode()).isNull();
        }

        @Test
        @DisplayName("should handle List data type")
        void shouldHandleListDataType() {
            List<String> items = List.of("item1", "item2", "item3");
            ApiResponse<List<String>> response = ApiResponse.ok(items);

            assertThat(response.success()).isTrue();
            assertThat(response.data()).containsExactly("item1", "item2", "item3");
            assertThat(response.error()).isNull();
        }

        @Test
        @DisplayName("should handle Integer data type")
        void shouldHandleIntegerDataType() {
            ApiResponse<Integer> response = ApiResponse.ok(42);

            assertThat(response.success()).isTrue();
            assertThat(response.data()).isEqualTo(42);
        }

        @Test
        @DisplayName("should handle DTO data type")
        void shouldHandleDtoDataType() {
            AuthorDTO authorDto = new AuthorDTO(
                "id-1", "Eric Evans", "bio", null, null, "American", null, null, null, null
            );
            ApiResponse<AuthorDTO> response = ApiResponse.ok(authorDto);

            assertThat(response.success()).isTrue();
            assertThat(response.data().name()).isEqualTo("Eric Evans");
        }

        @Test
        @DisplayName("should handle Void data type for no-content success")
        void shouldHandleVoidDataType() {
            ApiResponse<Void> response = ApiResponse.ok(null);

            assertThat(response.success()).isTrue();
            assertThat(response.data()).isNull();
        }
    }

    // ---------------------------------------------------------------
    // ApiResponse.error() factory
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("ApiResponse.error() factory")
    class ErrorFactoryTests {

        @Test
        @DisplayName("should create error response with error code and message")
        void shouldCreateErrorResponse() {
            ApiResponse<Void> response = ApiResponse.error("VALIDATION_ERROR", "Field is required");

            assertThat(response.success()).isFalse();
            assertThat(response.error()).isEqualTo("Field is required");
            assertThat(response.errorCode()).isEqualTo("VALIDATION_ERROR");
        }

        @Test
        @DisplayName("should set data to null for error response")
        void shouldSetDataToNullForErrorResponse() {
            ApiResponse<Void> response = ApiResponse.error("NOT_FOUND", "Resource not found");

            assertThat(response.data()).isNull();
        }

        @Test
        @DisplayName("should handle NOT_FOUND error code")
        void shouldHandleNotFoundError() {
            ApiResponse<Void> response = ApiResponse.error("BOOK_NOT_FOUND", "Book with id 123 not found");

            assertThat(response.success()).isFalse();
            assertThat(response.errorCode()).isEqualTo("BOOK_NOT_FOUND");
            assertThat(response.error()).isEqualTo("Book with id 123 not found");
            assertThat(response.data()).isNull();
        }

        @Test
        @DisplayName("should handle DUPLICATE error code")
        void shouldHandleDuplicateError() {
            ApiResponse<Void> response = ApiResponse.error("DUPLICATE_ISBN", "ISBN already exists");

            assertThat(response.success()).isFalse();
            assertThat(response.errorCode()).isEqualTo("DUPLICATE_ISBN");
            assertThat(response.error()).isEqualTo("ISBN already exists");
        }

        @Test
        @DisplayName("should handle VALIDATION_ERROR error code")
        void shouldHandleValidationError() {
            ApiResponse<Void> response = ApiResponse.error("VALIDATION_ERROR", "title: must not be blank; isbn: invalid format");

            assertThat(response.success()).isFalse();
            assertThat(response.errorCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.error()).contains("title");
            assertThat(response.error()).contains("isbn");
        }

        @Test
        @DisplayName("should handle null error code")
        void shouldHandleNullErrorCode() {
            ApiResponse<Void> response = ApiResponse.error(null, "Some error");

            assertThat(response.success()).isFalse();
            assertThat(response.errorCode()).isNull();
            assertThat(response.error()).isEqualTo("Some error");
        }

        @Test
        @DisplayName("should handle null error message")
        void shouldHandleNullErrorMessage() {
            ApiResponse<Void> response = ApiResponse.error("ERROR", null);

            assertThat(response.success()).isFalse();
            assertThat(response.error()).isNull();
            assertThat(response.errorCode()).isEqualTo("ERROR");
        }
    }

    // ---------------------------------------------------------------
    // Record behavior
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("ApiResponse record behavior")
    class RecordBehaviorTests {

        @Test
        @DisplayName("should have correct equals for success responses with same data")
        void shouldHaveCorrectEqualsForSuccessResponses() {
            ApiResponse<String> response1 = ApiResponse.ok("data");
            ApiResponse<String> response2 = ApiResponse.ok("data");

            assertThat(response1).isEqualTo(response2);
        }

        @Test
        @DisplayName("should have correct equals for error responses with same fields")
        void shouldHaveCorrectEqualsForErrorResponses() {
            ApiResponse<Void> response1 = ApiResponse.error("CODE", "message");
            ApiResponse<Void> response2 = ApiResponse.error("CODE", "message");

            assertThat(response1).isEqualTo(response2);
        }

        @Test
        @DisplayName("should not be equal for different data")
        void shouldNotBeEqualForDifferentData() {
            ApiResponse<String> response1 = ApiResponse.ok("data1");
            ApiResponse<String> response2 = ApiResponse.ok("data2");

            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("should not be equal for ok vs error")
        void shouldNotBeEqualForOkVsError() {
            ApiResponse<String> okResponse = ApiResponse.ok("data");
            ApiResponse<String> errorResponse = new ApiResponse<>(false, "data", "err", "CODE");

            assertThat(okResponse).isNotEqualTo(errorResponse);
        }

        @Test
        @DisplayName("should have consistent hashCode")
        void shouldHaveConsistentHashCode() {
            ApiResponse<String> response = ApiResponse.ok("data");

            assertThat(response.hashCode()).isEqualTo(response.hashCode());
        }
    }
}
