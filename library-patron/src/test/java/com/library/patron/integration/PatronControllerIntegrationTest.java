package com.library.patron.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.patron.application.dto.ApiResponse;
import com.library.patron.application.dto.PatronDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatronControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterPatronSuccessfully() throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
            "firstName", "John",
            "lastName", "Doe",
            "email", "john@example.com",
            "phone", "555-1234",
            "address", "123 Main St",
            "city", "Springfield",
            "postalCode", "62701",
            "patronType", "STUDENT"
        ));

        mockMvc.perform(post("/api/patrons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").isNotEmpty())
            .andExpect(jsonPath("$.data.firstName").value("John"))
            .andExpect(jsonPath("$.data.lastName").value("Doe"))
            .andExpect(jsonPath("$.data.email").value("john@example.com"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.patronType").value("STUDENT"))
            .andExpect(jsonPath("$.data.currentLoans").value(0))
            .andExpect(jsonPath("$.data.outstandingFines").value(0.00))
            .andExpect(jsonPath("$.data.memberSince").isNotEmpty());
    }

    @Test
    void shouldGetPatronById() throws Exception {
        String patronId = registerPatron("Jane", "Smith", "jane@example.com", "STUDENT");

        mockMvc.perform(get("/api/patrons/{id}", patronId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(patronId))
            .andExpect(jsonPath("$.data.firstName").value("Jane"))
            .andExpect(jsonPath("$.data.lastName").value("Smith"))
            .andExpect(jsonPath("$.data.email").value("jane@example.com"));
    }

    @Test
    void shouldGetAllPatrons() throws Exception {
        registerPatron("Alice", "Brown", "alice@example.com", "STUDENT");
        registerPatron("Bob", "Wilson", "bob@example.com", "FACULTY");

        mockMvc.perform(get("/api/patrons"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void shouldUpdatePatronSuccessfully() throws Exception {
        String patronId = registerPatron("Old", "Name", "old@example.com", "STUDENT");

        String updateBody = objectMapper.writeValueAsString(Map.of(
            "firstName", "New",
            "lastName", "Name",
            "email", "new@example.com",
            "phone", "555-9999",
            "address", "456 Oak Ave",
            "city", "Shelbyville",
            "postalCode", "62565"
        ));

        mockMvc.perform(put("/api/patrons/{id}", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(patronId))
            .andExpect(jsonPath("$.data.firstName").value("New"))
            .andExpect(jsonPath("$.data.lastName").value("Name"))
            .andExpect(jsonPath("$.data.email").value("new@example.com"))
            .andExpect(jsonPath("$.data.phone").value("555-9999"))
            .andExpect(jsonPath("$.data.city").value("Shelbyville"));
    }

    @Test
    void shouldSuspendPatronSuccessfully() throws Exception {
        String patronId = registerPatron("Suspend", "Test", "suspend@example.com", "STUDENT");

        String suspendBody = objectMapper.writeValueAsString(Map.of("reason", "Policy violation"));

        mockMvc.perform(post("/api/patrons/{id}/suspend", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(suspendBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify status changed
        mockMvc.perform(get("/api/patrons/{id}", patronId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }

    @Test
    void shouldReactivatePatronSuccessfully() throws Exception {
        String patronId = registerPatron("Reactivate", "Test", "reactivate@example.com", "STUDENT");

        // First suspend
        mockMvc.perform(post("/api/patrons/{id}/suspend", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("reason", "Test suspension"))))
            .andExpect(status().isOk());

        // Then reactivate
        String reactivateBody = objectMapper.writeValueAsString(Map.of("reason", "Issue resolved"));

        mockMvc.perform(post("/api/patrons/{id}/reactivate", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reactivateBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify status changed back
        mockMvc.perform(get("/api/patrons/{id}", patronId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void shouldTerminatePatronSuccessfully() throws Exception {
        String patronId = registerPatron("Terminate", "Test", "terminate@example.com", "STUDENT");

        String terminateBody = objectMapper.writeValueAsString(Map.of("reason", "Membership ended"));

        mockMvc.perform(post("/api/patrons/{id}/terminate", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(terminateBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify status changed
        mockMvc.perform(get("/api/patrons/{id}", patronId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("TERMINATED"));
    }

    @Test
    void shouldAddFineSuccessfully() throws Exception {
        String patronId = registerPatron("Fine", "Test", "fine@example.com", "STUDENT");

        String fineBody = objectMapper.writeValueAsString(Map.of(
            "amount", "10.00",
            "reason", "Late return"
        ));

        mockMvc.perform(post("/api/patrons/{id}/fines", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(fineBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify fine added
        mockMvc.perform(get("/api/patrons/{id}", patronId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.outstandingFines").value(10.00));
    }

    @Test
    void shouldPayFineSuccessfully() throws Exception {
        String patronId = registerPatron("PayFine", "Test", "payfine@example.com", "STUDENT");

        // Add a fine first
        mockMvc.perform(post("/api/patrons/{id}/fines", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "amount", "25.00",
                    "reason", "Late return"
                ))))
            .andExpect(status().isOk());

        // Pay part of the fine
        String payBody = objectMapper.writeValueAsString(Map.of("amount", "15.00"));

        mockMvc.perform(post("/api/patrons/{id}/fines/pay", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify remaining fines
        mockMvc.perform(get("/api/patrons/{id}", patronId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.outstandingFines").value(10.00));
    }

    @Test
    void shouldWaiveFineSuccessfully() throws Exception {
        String patronId = registerPatron("WaiveFine", "Test", "waivefine@example.com", "STUDENT");

        // Add a fine first
        mockMvc.perform(post("/api/patrons/{id}/fines", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "amount", "20.00",
                    "reason", "Late return"
                ))))
            .andExpect(status().isOk());

        // Waive part of the fine
        String waiveBody = objectMapper.writeValueAsString(Map.of(
            "amount", "20.00",
            "reason", "Administrative waiver"
        ));

        mockMvc.perform(post("/api/patrons/{id}/fines/waive", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(waiveBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify fines cleared
        mockMvc.perform(get("/api/patrons/{id}", patronId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.outstandingFines").value(0.00));
    }

    @Test
    void shouldChangePatronTypeSuccessfully() throws Exception {
        String patronId = registerPatron("TypeChange", "Test", "typechange@example.com", "STUDENT");

        // Verify initial type and privileges
        MvcResult initialResult = mockMvc.perform(get("/api/patrons/{id}", patronId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.patronType").value("STUDENT"))
            .andExpect(jsonPath("$.data.maxLoans").value(5))
            .andReturn();

        String typeBody = objectMapper.writeValueAsString(Map.of("patronType", "FACULTY"));

        mockMvc.perform(put("/api/patrons/{id}/type", patronId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(typeBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify type changed and privileges updated
        mockMvc.perform(get("/api/patrons/{id}", patronId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.patronType").value("FACULTY"))
            .andExpect(jsonPath("$.data.maxLoans").value(20));
    }

    @Test
    void shouldReturn404WhenPatronNotFound() throws Exception {
        String invalidId = "nonexistent-id-12345";

        mockMvc.perform(get("/api/patrons/{id}", invalidId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("PATRON_NOT_FOUND"));
    }

    @Test
    void shouldReturn409WhenDuplicateEmail() throws Exception {
        registerPatron("First", "User", "duplicate@example.com", "STUDENT");

        String duplicateBody = objectMapper.writeValueAsString(Map.of(
            "firstName", "Second",
            "lastName", "User",
            "email", "duplicate@example.com",
            "patronType", "FACULTY"
        ));

        mockMvc.perform(post("/api/patrons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateBody))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("DUPLICATE_EMAIL"));
    }

    // --- Helper methods ---

    private String registerPatron(String firstName, String lastName, String email, String patronType) throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
            "firstName", firstName,
            "lastName", lastName,
            "email", email,
            "patronType", patronType
        ));

        MvcResult result = mockMvc.perform(post("/api/patrons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn();

        ApiResponse<PatronDTO> response = readPatronResponse(result);
        return response.getData().getId();
    }

    @SuppressWarnings("unchecked")
    private ApiResponse<PatronDTO> readPatronResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        return objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, PatronDTO.class));
    }
}
