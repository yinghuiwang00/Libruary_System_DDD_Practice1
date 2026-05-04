package com.library.catalog.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.catalog.application.dto.ApiResponse;
import com.library.catalog.application.dto.BookDTO;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class BookUpdateSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    private String bookId;
    private MvcResult mvcResult;

    @Given("系统中存在一本状态为\"PUBLISHED\"的图书")
    public void publishedBookExists() {
        Book book = Book.create(
            new ISBN("978-7-111-40701-0"),
            "领域驱动设计", "Test description", null, 400, "zh"
        );
        book.addAuthor("author-001", "Eric Evans", AuthorRole.AUTHOR);
        book.setPublisher("publisher-001");
        book.addCategory("cat-001");
        book.publish();
        Book saved = bookRepository.save(book);
        bookId = saved.getId().getValue();
    }

    @When("我将该图书的标题更新为\"{string}\"")
    public void updateBookTitle(String newTitle) throws Exception {
        Map<String, Object> command = Map.of("title", newTitle);
        mvcResult = mockMvc.perform(put("/api/catalog/books/{id}", bookId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(command)))
            .andReturn();
    }

    @Then("图书更新成功")
    public void bookUpdated() {
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
    }

    @Then("图书标题为\"{string}\"")
    @SuppressWarnings("unchecked")
    public void bookTitleIs(String expectedTitle) throws Exception {
        ApiResponse<BookDTO> response = objectMapper.readValue(
            mvcResult.getResponse().getContentAsString(),
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, BookDTO.class)
        );
        assertThat(response.success()).isTrue();
        assertThat(response.data().title()).isEqualTo(expectedTitle);
    }
}
