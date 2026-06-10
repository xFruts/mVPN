package ru.maxow.mvpn.util.exception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.RequestEntity.post;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GlobalExceptionHandler - unit tests")
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  @DisplayName("Given NotFoundException When handle Then return NOT_FOUND with existing correlation id")
  void givenNotFoundWhenHandleThenNotFoundResponse() {
    MDC.put("correlation-id", "cid-1");
    WebRequest request = webRequest("uri=/v1/users/1?debug=true");

    ErrorResponse response = handler.handleNotFoundException(new NotFoundException("User", 1L), request);

    assertThat(response.getErrorCode()).isEqualTo("NOT_FOUND");
    assertThat(response.getMessage()).isEqualTo("User with identifier [1] not found");
    assertThat(response.getCorrelationId()).isEqualTo("cid-1");
  }

  @Test
  @DisplayName("Given ResourceAlreadyExistsException When handle Then return RESOURCE_ALREADY_EXISTS")
  void givenResourceAlreadyExistsWhenHandleThenReturnConflictContract() {
    ErrorResponse response = handler.handleResourceAlreadyExistsException(
        new ResourceAlreadyExistsException("duplicate user"),
        webRequest("uri=/v1/users")
    );

    assertThat(response.getErrorCode()).isEqualTo("RESOURCE_ALREADY_EXISTS");
    assertThat(response.getMessage()).isEqualTo("Resource already exists");
    assertThat(response.getCorrelationId()).isNotBlank();
  }

  @Test
  @DisplayName("Given missing route exception When handle Then return RESOURCE_NOT_FOUND")
  void givenNoResourceWhenHandleThenResourceNotFound() {
    WebRequest request = webRequest("uri=/missing/path");
    NoResourceFoundException ex = mock(NoResourceFoundException.class);

    ErrorResponse response = handler.handleNoResourceFoundException(ex, request);

    assertThat(response.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
    assertThat(response.getMessage()).isEqualTo("Resource not found");
    assertThat(response.getCorrelationId()).isNotBlank();
  }

  @Test
  @DisplayName("Given method not allowed exception When handle Then return METHOD_NOT_ALLOWED")
  void givenMethodNotAllowedWhenHandleThenMethodNotAllowed() {
    WebRequest request = webRequest("uri=/v1/users/1");

    ErrorResponse response = handler.handleHttpRequestNotSupportedException(
        new HttpRequestMethodNotSupportedException("POST"),
        request
    );

    assertThat(response.getErrorCode()).isEqualTo("METHOD_NOT_ALLOWED");
    assertThat(response.getMessage()).isEqualTo("Method not allowed");
  }

  @Test
  @DisplayName("Given multipart and illegal argument exceptions When handle Then map to BAD_REQUEST and INVALID_FILE_UPLOAD")
  void givenBadRequestVariantsWhenHandleThenMapErrorCodes() {
    WebRequest request = webRequest("uri=/v1/upload");

    ErrorResponse illegalArgument = handler.handleBadRequestException(new IllegalArgumentException("bad"), request);
    ErrorResponse multipart = handler.handleBadRequestException(new MultipartException("invalid file"), request);

    assertThat(illegalArgument.getErrorCode()).isEqualTo("BAD_REQUEST");
    assertThat(illegalArgument.getMessage()).isEqualTo("Invalid request parameters");
    assertThat(multipart.getErrorCode()).isEqualTo("INVALID_FILE_UPLOAD");
  }

  @Test
  @DisplayName("Given validation errors with duplicate field When handle Then merge messages in fieldErrors")
  void givenValidationErrorsWhenHandleThenMergeDuplicateFieldMessages() {
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    when(ex.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors()).thenReturn(List.of(
        new FieldError("request", "name", "must not be blank"),
        new FieldError("request", "name", "size must be between 3 and 64"),
        new FieldError("request", "email", "must be well-formed")
    ));

    ValidationErrorResponse response = handler.handleValidation(ex, webRequest("uri=/v1/users"));

    assertThat(response.getErrorCode()).isEqualTo("VALIDATION_FAILED");
    assertThat(response.getFieldErrors().get("name")).isEqualTo("must not be blank; size must be between 3 and 64");
    assertThat(response.getFieldErrors().get("email")).isEqualTo("must be well-formed");
    assertThat(response.getCorrelationId()).isNotBlank();
  }

  @Test
  @DisplayName("Given XUI unavailable exception When handle Then return SERVICE_UNAVAILABLE contract")
  void givenXuiUnavailableWhenHandleThenReturn503Contract() {
    ErrorResponse response = handler.handleXuiUnavailableException(
        new XuiUnavailableException("xui down"),
        webRequest("uri=/v1/config/abc")
    );

    assertThat(response.getErrorCode()).isEqualTo("XUI_UNAVAILABLE");
    assertThat(response.getMessage()).isEqualTo("XUI service is temporarily unavailable");
    assertThat(response.getCorrelationId()).isNotBlank();
  }

  @Test
  @DisplayName("Given unexpected exception When handle Then return INTERNAL_ERROR with correlation id")
  void givenUnexpectedWhenHandleThenReturnInternalError() {
    ErrorResponse response = handler.handleGlobalException(
        new RuntimeException("boom"),
        webRequest("uri=/v1/unknown")
    );

    assertThat(response.getErrorCode()).isEqualTo("INTERNAL_ERROR");
    assertThat(response.getMessage()).isEqualTo("An unexpected error occurred");
    assertThat(response.getCorrelationId()).isNotBlank();
  }

  @Test
  @DisplayName("Given data integrity violation When handle Then return DATA_CONFLICT")
  void givenDataIntegrityViolationWhenHandleThenReturnConflictContract() {
    ErrorResponse response = handler.handleDataIntegrityViolationException(
        new DataIntegrityViolationException("duplicate key"),
        webRequest("uri=/v1/users")
    );

    assertThat(response.getErrorCode()).isEqualTo("DATA_CONFLICT");
    assertThat(response.getMessage()).isEqualTo("Data conflict occurred");
  }

  @Test
  @DisplayName("Given HttpMediaTypeNotSupportedException When handle Then return MEDIA_TYPE_NOT_SUPPORTED with existing correlation id")
  void givenMediaTypeNotSupportedWhenHandleThenUnsupportedMediaTypeResponse() {
    MDC.put("correlation-id", "cid-1");
    WebRequest request = webRequest("uri=/v1/servers/ssh-keys");

    HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(
        MediaType.APPLICATION_XML,
        List.of(MediaType.APPLICATION_JSON)
    );

    ErrorResponse response = handler.handleHttpMediaTypeNotSupported(ex, request);

    assertThat(response.getErrorCode()).isEqualTo("MEDIA_TYPE_NOT_SUPPORTED");
    assertThat(response.getMessage()).contains("application/xml");
    assertThat(response.getCorrelationId()).isEqualTo("cid-1");
  }

  private WebRequest webRequest(String description) {
    WebRequest request = mock(WebRequest.class);
    when(request.getDescription(false)).thenReturn(description);
    return request;
  }
}

