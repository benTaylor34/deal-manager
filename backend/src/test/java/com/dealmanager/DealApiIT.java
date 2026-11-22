package com.dealmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full stack against a real Postgres: HTTP → security → service → JPA → Flyway schema. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DealApiIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final RequestPostProcessor RM = httpBasic("rm.user", "demo");
    private static final RequestPostProcessor COMMITTEE = httpBasic("committee.user", "demo");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void committeeDealLifecycle() throws Exception {
        long clientId = json.readTree(mvc.perform(get("/api/clients").with(RM))
                .andReturn().getResponse().getContentAsString()).get(0).get("id").asLong();

        // Create: 201 + Location + ETag; money comes back as strings
        String body = """
                {"clientId": %d, "name": "Treasury bundle", "startDate": "2027-01-01", "endDate": "2027-12-31",
                 "pricingLines": [{"service": "Wires", "standardPrice": "25.00", "proposedPrice": "18.00",
                                   "committedVolume": 1200}]}""".formatted(clientId);
        String created = mvc.perform(post("/api/deals").with(RM).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.summary.discountRate").value("0.2800000000"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();

        // Committee can't create deals
        mvc.perform(post("/api/deals").with(COMMITTEE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        // Mutations need If-Match
        mvc.perform(post("/api/deals/{id}/submit", id).with(RM))
                .andExpect(status().isPreconditionRequired());

        // 28% off → committee
        mvc.perform(post("/api/deals/{id}/submit", id).with(RM).header("If-Match", "\"0\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_COMMITTEE"))
                .andExpect(header().string("ETag", "\"1\""));

        // RM can't approve their own committee deal
        mvc.perform(post("/api/deals/{id}/approve", id).with(RM).header("If-Match", "\"1\""))
                .andExpect(status().isForbidden());

        // Stale screen → 412
        mvc.perform(post("/api/deals/{id}/approve", id).with(COMMITTEE).header("If-Match", "\"0\""))
                .andExpect(status().isPreconditionFailed());

        // Reject without a reason → 400
        mvc.perform(post("/api/deals/{id}/reject", id).with(COMMITTEE).header("If-Match", "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/deals/{id}/approve", id).with(COMMITTEE).header("If-Match", "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comment\": \"Volume justifies it\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // APPROVED is terminal → 409
        mvc.perform(post("/api/deals/{id}/reject", id).with(COMMITTEE).header("If-Match", "\"2\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comment\": \"changed my mind\"}"))
                .andExpect(status().isConflict());

        JsonNode audit = json.readTree(mvc.perform(get("/api/deals/{id}/audit", id).with(RM))
                .andReturn().getResponse().getContentAsString());
        assertThat(audit).extracting(n -> n.get("action").asText())
                .containsExactly("CREATED", "SUBMITTED", "APPROVED");
        assertThat(audit.get(2).get("actor").asText()).isEqualTo("committee.user");
        assertThat(audit.get(2).get("reason").asText()).isEqualTo("Volume justifies it");
    }

    @Test
    void validationErrorsAre400WithFieldDetails() throws Exception {
        mvc.perform(post("/api/deals").with(RM).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": 1, "name": "", "startDate": "2027-01-01", "endDate": "2027-12-31",
                                 "pricingLines": [{"service": "Wires", "standardPrice": "-1", "proposedPrice": "1",
                                                   "committedVolume": 0}]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors['pricingLines[0].standardPrice']").exists())
                .andExpect(jsonPath("$.errors['pricingLines[0].committedVolume']").exists());
    }

    @Test
    void unknownDealIs404AndAnonymousIs401() throws Exception {
        mvc.perform(get("/api/deals/999999").with(RM)).andExpect(status().isNotFound());
        mvc.perform(get("/api/deals")).andExpect(status().isUnauthorized());
    }
}
