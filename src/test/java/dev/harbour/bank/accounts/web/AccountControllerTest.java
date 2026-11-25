package dev.harbour.bank.accounts.web;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.harbour.bank.accounts.application.AccountService;
import dev.harbour.bank.accounts.domain.Account;
import dev.harbour.bank.accounts.domain.AccountStatus;
import dev.harbour.bank.accounts.domain.AccountType;
import dev.harbour.bank.shared.error.BusinessRuleException;
import dev.harbour.bank.shared.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AccountService service;

    private Account newAccount(UUID customerId) {
        return Account.open(customerId, "HB-1000000000", AccountType.CHEQUING, Currency.getInstance("CAD"), CLOCK);
    }

    @Test
    void openReturns201WithLocationHeader() throws Exception {
        UUID customerId = UUID.randomUUID();
        Account account = newAccount(customerId);
        given(service.open(customerId, AccountType.CHEQUING, "CAD")).willReturn(account);

        mvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": "%s", "type": "CHEQUING", "currency": "CAD"}"""
                                .formatted(customerId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/accounts/" + account.id())))
                .andExpect(jsonPath("$.accountNumber").value("HB-1000000000"))
                .andExpect(jsonPath("$.currency").value("CAD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void invalidBodyReturns400ProblemWithFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "CHEQUING", "currency": "cad"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors", hasSize(2))); // missing customerId, lowercase currency

        verifyNoInteractions(service);
    }

    @Test
    void unknownAccountTypeReturns400() throws Exception {
        mvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": "%s", "type": "PIGGYBANK", "currency": "CAD"}"""
                                .formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void businessRuleViolationReturns422() throws Exception {
        UUID customerId = UUID.randomUUID();
        given(service.open(customerId, AccountType.CHEQUING, "CAD"))
                .willThrow(new BusinessRuleException("Customer already has the maximum of 3 open accounts"));

        mvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": "%s", "type": "CHEQUING", "currency": "CAD"}"""
                                .formatted(customerId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value("Customer already has the maximum of 3 open accounts"));
    }

    @Test
    void getUnknownAccountReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        given(service.get(id)).willThrow(new NotFoundException("Account", id));

        mvc.perform(get("/api/v1/accounts/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account not found: " + id));
    }

    @Test
    void listCanFilterByCustomer() throws Exception {
        UUID customerId = UUID.randomUUID();
        given(service.list(customerId)).willReturn(List.of(newAccount(customerId)));

        mvc.perform(get("/api/v1/accounts").param("customerId", customerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerId").value(customerId.toString()));
    }

    @Test
    void patchStatusFreezesAccount() throws Exception {
        Account account = newAccount(UUID.randomUUID());
        account.changeStatus(AccountStatus.FROZEN);
        given(service.changeStatus(account.id(), AccountStatus.FROZEN)).willReturn(account);

        mvc.perform(patch("/api/v1/accounts/{id}/status", account.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "FROZEN"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }

    @Test
    void patchStatusWithoutBodyFieldReturns400() throws Exception {
        mvc.perform(patch("/api/v1/accounts/{id}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)));
    }
}
