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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class BookQuerySteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    private String bookId;
    private MvcResult mvcResult;

    @Given("系统中存在一本ISBN为\"{string}\"且状态为\"PUBLISHED\"的图书")
    public void publishedBookWithIsbnExists(String isbn) {
        Book book = Book.create(
            new ISBN(isbn),
            "领域驱动设计", "Test description", null, 400, "zh"
        );
        book.addAuthor("author-001", "Eric Evans", AuthorRole.AUTHOR);
        book.setPublisher("publisher-001");
        book.addCategory("cat-001");
        book.publish();
        Book saved = bookRepository.save(book);
        bookId = saved.getId().getValue();
    }

    @When("我通过该图书的ID查询图书")
    public void queryBookById() throws Exception {
        mvcResult = mockMvc.perform(get("/api/catalog/books/{id}", bookId)
                .contentType(MediaType.APPLICATION_JSON))
            .andReturn();
    }

    @Then("返回图书信息")
    public void bookInfoReturned() {
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
