package com.example.northstarmarket.status;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status")
class StatusController {

	private final ApplicationStatusService statusService;

	StatusController(ApplicationStatusService statusService) {
		this.statusService = statusService;
	}

	@GetMapping
	ApplicationStatus status() {
		return statusService.currentStatus();
	}
}
