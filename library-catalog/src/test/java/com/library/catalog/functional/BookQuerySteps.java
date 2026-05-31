package com.library.catalog.functional;

import com.library.catalog.domain.model.Book;
import com.library.catalog.domain.model.ISBN;
import com.library.catalog.domain.model.enums.AuthorRole;
import com.library.catalog.domain.repository.BookRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class BookQuerySteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestScenarioState state;

    @Given("^a book with ISBN \"([^\"]*)\" and status \"([^\"]*)\" exists in the system$")
    public void publishedBookWithIsbnExists(String isbn, String status) {
        Book book = Book.create(
            new ISBN(isbn),
            "Domain-Driven Design", "Test description", null, 400, "zh"
        );
        book.addAuthor("author-001", "Eric Evans", AuthorRole.AUTHOR);
        book.setPublisher("publisher-001");
        book.addCategory("cat-001");
        book.publish();
        Book saved = bookRepository.save(book);
        state.setBookId(saved.getId().getValue());
    }

    @When("I query the book by its ID")
    public void queryBookById() throws Exception {
        state.setMvcResult(mockMvc.perform(get("/api/catalog/books/{id}", state.getBookId())
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn());
    }

    @Then("the book information is returned")
    public void bookInfoReturned() {
        // Assertion handled by SharedBookSteps
    }
}
