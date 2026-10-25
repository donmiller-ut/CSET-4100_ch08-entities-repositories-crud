package com.example.northstarmarket.status;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApplicationStatusServiceTests {

	@Test
	void returnsTheConfiguredApplicationNameAndReadyStatus() {
		var service = new ApplicationStatusService("northstar-market");

		var status = service.currentStatus();

		assertThat(status.application()).isEqualTo("northstar-market");
		assertThat(status.status()).isEqualTo("ready");
	}
}
