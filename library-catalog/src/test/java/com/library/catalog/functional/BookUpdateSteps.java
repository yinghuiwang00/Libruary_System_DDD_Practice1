package com.library.catalog.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.catalog.application.command.UpdateBookCommand;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class BookUpdateSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestScenarioState state;

    @When("^I update the book title to \"([^\"]*)\"$")
    public void updateBookTitle(String newTitle) throws Exception {
        UpdateBookCommand command = new UpdateBookCommand(newTitle, null, null, null, null);
        state.setMvcResult(mockMvc.perform(put("/api/catalog/books/{id}", state.getBookId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andReturn());
    }

    @Then("the book is updated successfully")
    public void bookUpdated() {
        assertThat(state.getMvcResult().getResponse().getStatus()).isEqualTo(200);
    }
}
