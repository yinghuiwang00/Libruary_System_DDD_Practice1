package com.library.catalog.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.catalog.application.dto.ApiResponse;
import com.library.catalog.application.dto.BookDTO;
import com.library.catalog.domain.model.Book;
import com.library.catalog.domain.model.ISBN;
import com.library.catalog.domain.model.enums.AuthorRole;
import com.library.catalog.domain.model.enums.BookStatus;
import com.library.catalog.domain.repository.BookRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class SharedBookSteps {

    @Autowired
    private TestScenarioState state;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Given("^a book with status \"([^\"]*)\" exists in the system$")
    public void bookWithStatusExists(String status) {
        Book book = Book.create(
            new ISBN("9787111407027"),
            "Domain-Driven Design", "Test description", null, 400, "zh"
        );
        book.addAuthor("author-001", "Eric Evans", AuthorRole.AUTHOR);
        book.setPublisher("publisher-001");
        book.addCategory("cat-001");
        if (BookStatus.valueOf(status) == BookStatus.PUBLISHED) {
            book.publish();
        }
        Book saved = bookRepository.save(book);
        state.setBookId(saved.getId().getValue());
    }

    @Then("^the book title is \"([^\"]*)\"$")
    @SuppressWarnings("unchecked")
    public void bookTitleIs(String expectedTitle) throws Exception {
        MvcResult result = state.getMvcResult();
        ApiResponse<BookDTO> response = objectMapper.readValue(
            result.getResponse().getContentAsString(StandardCharsets.UTF_8),
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, BookDTO.class)
        );
        assertThat(response.success()).isTrue();
        assertThat(response.data().title()).isEqualTo(expectedTitle);
    }
}
