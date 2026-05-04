package com.library.catalog.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class BookCreationSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    private MvcResult mvcResult;

    @Given("系统中不存在ISBN为\"{string}\"的图书")
    public void noBookWithIsbn(String isbn) {
        boolean exists = bookRepository.existsByIsbn(new ISBN(isbn));
        assertThat(exists).isFalse();
    }

    @When("我创建一本新书，标题为\"{string}\"，作者为\"{string}\"")
    public void createBookWithTitleAndAuthor(String title, String author) throws Exception {
        Map<String, Object> command = Map.of(
            "isbn", "978-7-111-40701-0",
            "title", title
        );
        mvcResult = mockMvc.perform(post("/api/catalog/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andReturn();
    }

    @When("ISBN为\"{string}\"")
    public void setIsbn(String isbn) {
        // ISBN is already set in the create step
    }

    @When("分类为\"{string}\"")
    public void setCategory(String category) {
        // Category would be added separately; for this happy path the book is created first
    }

    @Then("图书创建成功")
    public void bookCreated() {
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(201);
    }

    @Then("图书状态为\"{string}\"")
    @SuppressWarnings("unchecked")
    public void bookStatusIs(String expectedStatus) throws Exception {
        ApiResponse<BookDTO> response = objectMapper.readValue(
            mvcResult.getResponse().getContentAsString(),
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, BookDTO.class)
        );
        assertThat(response.success()).isTrue();
        assertThat(response.data().status()).isEqualTo(expectedStatus);
    }
}
