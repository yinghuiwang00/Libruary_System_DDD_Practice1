package com.library.catalog.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.catalog.domain.model.Book;
import com.library.catalog.domain.model.ISBN;
import com.library.catalog.domain.model.enums.AuthorRole;
import com.library.catalog.domain.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void testCreateBookAPI() throws Exception {
        Map<String, Object> command = Map.of(
            "isbn", "978-7-111-40701-0",
            "title", "领域驱动设计",
            "description", "A book about DDD",
            "pageCount", 400,
            "language", "zh"
        );

        mockMvc.perform(post("/api/catalog/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.isbn").value("9787111407010"))
            .andExpect(jsonPath("$.data.title").value("领域驱动设计"))
            .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void testGetBookAPI() throws Exception {
        Book book = createAndSaveDraftBook("978-7-111-40701-0", "领域驱动设计");
        String bookId = book.getId().getValue();

        mockMvc.perform(get("/api/catalog/books/{id}", bookId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(bookId))
            .andExpect(jsonPath("$.data.title").value("领域驱动设计"));
    }

    @Test
    void testGetAllBooksAPI() throws Exception {
        createAndSaveDraftBook("978-7-111-40701-0", "领域驱动设计");
        createAndSaveDraftBook("978-7-111-54738-9", "实现领域驱动设计");

        mockMvc.perform(get("/api/catalog/books"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void testUpdateBookAPI() throws Exception {
        Book book = createAndSaveDraftBook("978-7-111-40701-0", "领域驱动设计");
        String bookId = book.getId().getValue();

        Map<String, Object> command = Map.of(
            "title", "领域驱动设计(修订版)",
            "description", "Updated description"
        );

        mockMvc.perform(put("/api/catalog/books/{id}", bookId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("领域驱动设计(修订版)"));
    }

    @Test
    void testPublishBookAPI() throws Exception {
        Book book = createPublishableDraftBook();
        String bookId = book.getId().getValue();

        mockMvc.perform(post("/api/catalog/books/{id}/publish", bookId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void testDeleteBookAPI() throws Exception {
        Book book = createAndSaveDraftBook("978-7-111-40701-0", "领域驱动设计");
        String bookId = book.getId().getValue();

        mockMvc.perform(delete("/api/catalog/books/{id}", bookId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        Book deleted = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(deleted.getStatus().name()).isEqualTo("DELETED");
    }

    // --- Helper methods ---

    private Book createAndSaveDraftBook(String isbn, String title) {
        Book book = Book.create(new ISBN(isbn), title, "Test description", null, 400, "zh");
        return bookRepository.save(book);
    }

    private Book createPublishableDraftBook() {
        Book book = Book.create(
            new ISBN("978-7-111-40701-0"),
            "领域驱动设计", "Test description", null, 400, "zh"
        );
        book.addAuthor("author-001", "Eric Evans", AuthorRole.AUTHOR);
        book.setPublisher("publisher-001");
        book.addCategory("cat-001");
        return bookRepository.save(book);
    }
}
