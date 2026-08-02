package com.gayuth.hephaestus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration test against a real Postgres (Testcontainers), with
 * security enabled. @ServiceConnection wires the datasource to the container,
 * overriding application.properties, so this runs anywhere Docker is available
 * (including GitHub Actions runners). Requires Docker to be running.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class HephaestusApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	MockMvc mvc;

	private static final String CASCADE = """
			{"traceId":"it-cascade","spans":[
			  {"spanId":"a","parentSpanId":null,"serviceName":"api-gateway","startTime":0,"duration":800,"status":"ERROR"},
			  {"spanId":"b","parentSpanId":"a","serviceName":"order-service","startTime":50,"duration":600,"status":"ERROR"},
			  {"spanId":"c","parentSpanId":"b","serviceName":"payment-service","startTime":100,"duration":400,"status":"ERROR"},
			  {"spanId":"d","parentSpanId":"c","serviceName":"database","startTime":150,"duration":120,"status":"ERROR"}
			]}""";

	@Test
	void contextLoads() {
	}

	@Test
	void unauthenticatedAnalyzeIsRejected() throws Exception {
		mvc.perform(post("/api/traces/analyze")
				.contentType("application/json").content(CASCADE))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void loginReturnsTokenAndRoles() throws Exception {
		mvc.perform(post("/api/auth/login")
				.contentType("application/json")
				.content("{\"username\":\"admin\",\"password\":\"admin\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value(notNullValue()))
				.andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
	}

	@Test
	@WithMockUser(username = "viewer", roles = { "VIEWER" })
	void viewerCannotAnalyze() throws Exception {
		mvc.perform(post("/api/traces/analyze")
				.contentType("application/json").content(CASCADE))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	void adminAnalyzesAndReportIsPersisted() throws Exception {
		mvc.perform(post("/api/traces/analyze")
				.contentType("application/json").content(CASCADE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mode").value("FAILURE"))
				.andExpect(jsonPath("$.failure.rootCauses[0]").value("database"));

		mvc.perform(get("/api/reports"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
				.andExpect(jsonPath("$[0].traceId").value("it-cascade"));
	}
}