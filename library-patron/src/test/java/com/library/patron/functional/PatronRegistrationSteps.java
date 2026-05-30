package com.library.patron.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.patron.application.dto.ApiResponse;
import com.library.patron.application.dto.PatronDTO;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class PatronRegistrationSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatronScenarioState state;

    @Given("no patron exists with email {string}")
    public void noPatronExistsWithEmail(String email) {
        // The database is cleaned before each scenario via CucumberSpringConfig,
        // so no patron with this email should exist.
    }

    @When("a registration request is made with the following details:")
    public void registrationRequestWithDetails(DataTable dataTable) throws Exception {
        Map<String, String> row = dataTable.asMaps().get(0);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("firstName", row.get("firstName"));
        requestBody.put("lastName", row.get("lastName"));
        requestBody.put("email", row.get("email"));
        requestBody.put("patronType", row.get("patronType"));

        state.setMvcResult(mockMvc.perform(post("/api/patrons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andReturn());

        ApiResponse<PatronDTO> response = readPatronResponse(state.getMvcResult());
        if (response.getData() != null) {
            state.setPatronId(response.getData().getId());
        }
    }

    @Then("the patron is successfully registered")
    public void patronSuccessfullyRegistered() {
        assertThat(state.getMvcResult().getResponse().getStatus()).isEqualTo(201);
    }

    @Then("the patron status is {string}")
    public void patronStatusIs(String expectedStatus) throws Exception {
        // Fetch fresh data from GET endpoint to confirm persisted state
        if (state.getPatronId() != null) {
            MvcResult getResult = mockMvc.perform(get("/api/patrons/{id}", state.getPatronId()))
                .andReturn();
            ApiResponse<PatronDTO> response = readPatronResponse(getResult);
            assertThat(response.getData().getStatus()).isEqualTo(expectedStatus);
        } else {
            ApiResponse<PatronDTO> response = readPatronResponse(state.getMvcResult());
            assertThat(response.getData().getStatus()).isEqualTo(expectedStatus);
        }
    }

    @SuppressWarnings("unchecked")
    private ApiResponse<PatronDTO> readPatronResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, PatronDTO.class));
    }
}
