package com.example.northstarmarket.status;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApplicationStatusService {

	private final String applicationName;

	public ApplicationStatusService(@Value("${spring.application.name}") String applicationName) {
		this.applicationName = applicationName;
	}

	public ApplicationStatus currentStatus() {
		return new ApplicationStatus(applicationName, "ready");
	}
}
