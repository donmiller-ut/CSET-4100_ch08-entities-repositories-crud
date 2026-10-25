package com.example.northstarmarket.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

public final class ApiProblem extends ProblemDetail {

	private final String code;
	private final String requestId;
	private final List<ErrorDetail> errors;

	ApiProblem(
			HttpStatusCode status,
			URI type,
			String title,
			String detail,
			String requestId,
			List<ErrorDetail> errors) {
		super(status.value());
		setType(type);
		setTitle(title);
		setDetail(detail);
		this.code = code(type);
		this.requestId = requestId;
		this.errors = List.copyOf(errors);
	}

	public String getCode() {
		return code;
	}

	public String getRequestId() {
		return requestId;
	}

	public List<ErrorDetail> getErrors() {
		return errors;
	}

	private String code(URI type) {
		String path = type.getPath();
		return path.substring(path.lastIndexOf('/') + 1).replace('-', '_');
	}
}
