package dev.harbour.bank.customers.web;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.harbour.bank.customers.application.CustomerService;
import dev.harbour.bank.customers.domain.Customer;
import dev.harbour.bank.shared.error.ConflictException;
import dev.harbour.bank.shared.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer slice test: only controllers and the exception handler are loaded, and the service is
 * replaced by a Mockito mock. No database and no Docker needed.
 */
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CustomerService service;

    @Test
    void registerReturns201WithLocationHeader() throws Exception {
        Customer customer = Customer.register("Ada Lovelace", "ada@example.com", CLOCK);
        given(service.register("Ada Lovelace", "ada@example.com")).willReturn(customer);

        mvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "Ada Lovelace", "email": "ada@example.com"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/customers/" + customer.id())))
                .andExpect(jsonPath("$.id").value(customer.id().toString()))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void invalidBodyReturns400ProblemWithFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "", "email": "not-an-email"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasSize(2)));

        verifyNoInteractions(service);
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        given(service.register("Ada Lovelace", "ada@example.com"))
                .willThrow(new ConflictException("A customer with this email already exists"));

        mvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "Ada Lovelace", "email": "ada@example.com"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("A customer with this email already exists"));
    }

    @Test
    void getUnknownCustomerReturns404Problem() throws Exception {
        UUID id = UUID.randomUUID();
        given(service.get(id)).willThrow(new NotFoundException("Customer", id));

        mvc.perform(get("/api/v1/customers/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Customer not found: " + id));
    }

    @Test
    void malformedIdReturns400() throws Exception {
        mvc.perform(get("/api/v1/customers/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void listReturnsAllCustomers() throws Exception {
        Customer ada = Customer.register("Ada Lovelace", "ada@example.com", CLOCK);
        Customer alan = Customer.register("Alan Turing", "alan@example.com", CLOCK);
        given(service.list()).willReturn(List.of(ada, alan));

        mvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$[1].fullName").value("Alan Turing"));
    }
}
