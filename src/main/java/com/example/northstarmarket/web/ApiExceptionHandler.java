package com.example.northstarmarket.web;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

import com.example.northstarmarket.product.ProductNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public final class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final String TYPE_ROOT = "https://api.northstar.example/problems/";

	@ExceptionHandler(ProductNotFoundException.class)
	ResponseEntity<Object> handleProductNotFound(
			ProductNotFoundException exception,
			WebRequest request) {
		return response(
				HttpStatus.NOT_FOUND,
				"product-not-found",
				"Product not found",
				exception.getMessage(),
				List.of(),
				request);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<Object> handleUnexpected(Exception exception, WebRequest request) {
		return response(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"internal-error",
				"Internal server error",
				"The server could not complete the request.",
				List.of(),
				request);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		List<ErrorDetail> errors = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorDetail(error.getField(), error.getDefaultMessage()))
				.sorted(Comparator.comparing(ErrorDetail::field).thenComparing(ErrorDetail::issue))
				.toList();
		return response(
				HttpStatus.UNPROCESSABLE_CONTENT,
				"validation-failed",
				"Request validation failed",
				"One or more request fields are invalid.",
				errors,
				request);
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(
			HandlerMethodValidationException exception,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		if (exception.isForReturnValue()) {
			return handleUnexpected(exception, request);
		}
		boolean requestBodyFailed = exception.getParameterValidationResults().stream()
				.anyMatch(result -> result.getMethodParameter().hasParameterAnnotation(RequestBody.class));
		if (requestBodyFailed) {
			List<ErrorDetail> errors = exception.getParameterValidationResults().stream()
					.filter(ParameterErrors.class::isInstance)
					.map(ParameterErrors.class::cast)
					.flatMap(result -> result.getFieldErrors().stream())
					.map(error -> new ErrorDetail(error.getField(), error.getDefaultMessage()))
					.sorted(Comparator.comparing(ErrorDetail::field).thenComparing(ErrorDetail::issue))
					.toList();
			return response(
					HttpStatus.UNPROCESSABLE_CONTENT,
					"validation-failed",
					"Request validation failed",
					"One or more request fields are invalid.",
					errors,
					request);
		}
		return response(
				HttpStatus.BAD_REQUEST,
				"invalid-parameter",
				"Invalid request parameter",
				"A path or query parameter is outside its allowed range.",
				List.of(),
				request);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception exception,
			Object body,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		if (body instanceof ApiProblem) {
			return super.handleExceptionInternal(exception, body, headers, status, request);
		}
		return response(
				status,
				codeFor(status),
				titleFor(status),
				detailFor(status),
				List.of(),
				request, headers);
	}

	private ResponseEntity<Object> response(
			HttpStatusCode status,
			String code,
			String title,
			String detail,
			List<ErrorDetail> errors,
			WebRequest request) {
		return response(status, code, title, detail, errors, request, HttpHeaders.EMPTY);
	}

	private ResponseEntity<Object> response(
			HttpStatusCode status, String code, String title, String detail,
			List<ErrorDetail> errors, WebRequest request, HttpHeaders headers) {
		ApiProblem problem = new ApiProblem(
				status,
				URI.create(TYPE_ROOT + code),
				title,
				detail,
				requestId(request),
				errors);
		problem.setInstance(URI.create(((ServletWebRequest) request).getRequest().getRequestURI()));
		return ResponseEntity.status(status).headers(headers).body(problem);
	}

	private String requestId(WebRequest request) {
		HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
		return (String) servletRequest.getAttribute(RequestIdFilter.ATTRIBUTE);
	}

	private String codeFor(HttpStatusCode status) {
		return switch (status.value()) {
			case 400 -> "malformed-request";
			case 404 -> "resource-not-found";
			case 406 -> "not-acceptable";
			case 415 -> "unsupported-media-type";
			default -> status.is5xxServerError() ? "internal-error" : "request-rejected";
		};
	}

	private String titleFor(HttpStatusCode status) {
		return HttpStatus.valueOf(status.value()).getReasonPhrase();
	}

	private String detailFor(HttpStatusCode status) {
		return switch (status.value()) {
			case 400 -> "The request could not be read or contains an invalid value.";
			case 404 -> "No resource matches the requested URI.";
			case 406 -> "The requested response representation is not available.";
			case 415 -> "The request body must use application/json.";
			default -> status.is5xxServerError()
					? "The server could not complete the request."
					: "The request could not be completed.";
		};
	}
}
