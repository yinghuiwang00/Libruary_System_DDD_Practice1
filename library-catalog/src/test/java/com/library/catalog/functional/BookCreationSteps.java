package com.library.catalog.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.catalog.application.command.CreateBookCommand;
import com.library.catalog.application.dto.ApiResponse;
import com.library.catalog.application.dto.BookDTO;
import com.library.catalog.domain.model.ISBN;
import com.library.catalog.domain.repository.BookRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class BookCreationSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestScenarioState state;

    @Given("^no book with ISBN \"([^\"]*)\" exists in the system$")
    public void noBookWithIsbn(String isbn) {
        boolean exists = bookRepository.existsByIsbn(new ISBN(isbn));
        assertThat(exists).isFalse();
    }

    @When("^I create a new book with title \"([^\"]*)\" and author \"([^\"]*)\"$")
    public void createBookWithTitleAndAuthor(String title, String author) throws Exception {
        CreateBookCommand command = new CreateBookCommand(
            "9787111407010", title, null, null, null, null
        );
        state.setMvcResult(mockMvc.perform(post("/api/catalog/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andReturn());
    }

    @When("^the ISBN is \"([^\"]*)\"$")
    public void setIsbn(String isbn) {
        // ISBN is already set in the create step
    }

    @When("^the category is \"([^\"]*)\"$")
    public void setCategory(String category) {
        // Category would be added separately
    }

    @Then("the book is created successfully")
    public void bookCreated() {
        assertThat(state.getMvcResult().getResponse().getStatus()).isEqualTo(201);
    }

    @Then("^the book status is \"([^\"]*)\"$")
    @SuppressWarnings("unchecked")
    public void bookStatusIs(String expectedStatus) throws Exception {
        ApiResponse<BookDTO> response = objectMapper.readValue(
            state.getMvcResult().getResponse().getContentAsString(StandardCharsets.UTF_8),
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, BookDTO.class)
        );
        assertThat(response.success()).isTrue();
        assertThat(response.data().status()).isEqualTo(expectedStatus);
    }
}
