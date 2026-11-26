package dev.harbour.bank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.harbour.bank.support.PostgresTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The whole application, every layer, against a real PostgreSQL: HTTP request in, controller,
 * transactional service, JPA, SQL, and back. Afterwards we look directly at the database tables to
 * prove the data was really stored there.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiEndToEndTest extends PostgresTestSupport {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void customerAndAccountLifecycleIsStoredInPostgres() throws Exception {
        // A unique email per run, because this test commits for real (no rollback here).
        String email = "e2e-" + UUID.randomUUID() + "@example.com";

        String customerBody = mvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "End To End", "email": "%s"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID customerId = UUID.fromString(JsonPath.read(customerBody, "$.id"));

        assertThat(jdbc.queryForObject("select count(*) from customers where email = ?", Integer.class, email))
                .isEqualTo(1);

        // Same email in a different case: rejected (409), and still only one row.
        mvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "Duplicate", "email": "%s"}""".formatted(email.toUpperCase())))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from customers where email = ?", Integer.class, email))
                .isEqualTo(1);

        String accountBody = mvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": "%s", "type": "SAVINGS", "currency": "CAD"}""".formatted(customerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value(org.hamcrest.Matchers.matchesPattern("HB-\\d{10}")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID accountId = UUID.fromString(JsonPath.read(accountBody, "$.id"));

        assertThat(jdbc.queryForObject("select status from accounts where id = ?", String.class, accountId))
                .isEqualTo("ACTIVE");

        mvc.perform(patch("/api/v1/accounts/{id}/status", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "FROZEN"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));
        assertThat(jdbc.queryForObject("select status from accounts where id = ?", String.class, accountId))
                .isEqualTo("FROZEN");
        assertThat(jdbc.queryForObject("select version from accounts where id = ?", Long.class, accountId))
                .isEqualTo(1L);

        // Closing is final: reopening is a business-rule violation (422) and changes nothing.
        mvc.perform(patch("/api/v1/accounts/{id}/status", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "CLOSED"}"""))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/accounts/{id}/status", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "ACTIVE"}"""))
                .andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("select status from accounts where id = ?", String.class, accountId))
                .isEqualTo("CLOSED");

        mvc.perform(get("/api/v1/accounts").param("customerId", customerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(accountId.toString()));

        mvc.perform(get("/api/v1/customers/{id}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }
}
