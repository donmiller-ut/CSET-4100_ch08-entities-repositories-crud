package com.example.northstarmarket.web;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public final class RequestIdFilter extends OncePerRequestFilter {

	public static final String ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";
	public static final String HEADER = "X-Request-ID";

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String requestId = "req-" + UUID.randomUUID().toString().replace("-", "");
		request.setAttribute(ATTRIBUTE, requestId);
		response.setHeader(HEADER, requestId);
		filterChain.doFilter(request, response);
	}
}
