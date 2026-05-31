package com.library.catalog.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.catalog.application.dto.ApiResponse;
import com.library.catalog.application.dto.BookDTO;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class BookPublishingSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestScenarioState state;

    @When("I publish the book")
    public void publishBook() throws Exception {
        state.setMvcResult(mockMvc.perform(post("/api/catalog/books/{id}/publish", state.getBookId())
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn());
    }

    @Then("^the book status becomes \"([^\"]*)\"$")
    @SuppressWarnings("unchecked")
    public void bookStatusBecomes(String expectedStatus) throws Exception {
        assertThat(state.getMvcResult().getResponse().getStatus()).isEqualTo(200);
        ApiResponse<BookDTO> response = objectMapper.readValue(
            state.getMvcResult().getResponse().getContentAsString(StandardCharsets.UTF_8),
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, BookDTO.class)
        );
        assertThat(response.success()).isTrue();
        assertThat(response.data().status()).isEqualTo(expectedStatus);
    }
}
