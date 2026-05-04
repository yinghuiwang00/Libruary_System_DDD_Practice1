package com.library.catalog.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.catalog.domain.model.Author;
import com.library.catalog.domain.repository.AuthorRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthorRepository authorRepository;

    @Nested
    class CreateAuthor {

        @Test
        void testCreateAuthor() throws Exception {
            Map<String, Object> request = Map.of(
                "name", "Eric Evans",
                "biography", "Author of Domain-Driven Design",
                "birthDate", "1957-01-01",
                "nationality", "American"
            );

            mockMvc.perform(post("/api/catalog/authors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Eric Evans"))
                .andExpect(jsonPath("$.data.biography").value("Author of Domain-Driven Design"))
                .andExpect(jsonPath("$.data.birthDate").value("1957-01-01"))
                .andExpect(jsonPath("$.data.nationality").value("American"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
        }

        @Test
        void testCreateAuthorWithMinimalData() throws Exception {
            Map<String, Object> request = Map.of(
                "name", "Robert C. Martin"
            );

            mockMvc.perform(post("/api/catalog/authors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Robert C. Martin"))
                .andExpect(jsonPath("$.data.biography").isEmpty())
                .andExpect(jsonPath("$.data.nationality").isEmpty());
        }
    }

    @Nested
    class GetAuthor {

        @Test
        void testGetAuthor() throws Exception {
            Author author = createAndSaveAuthor(
                "Eric Evans", "Author of DDD",
                LocalDate.of(1957, 1, 1), null, "American"
            );
            String authorId = author.getId().getValue();

            mockMvc.perform(get("/api/catalog/authors/{id}", authorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(authorId))
                .andExpect(jsonPath("$.data.name").value("Eric Evans"))
                .andExpect(jsonPath("$.data.biography").value("Author of DDD"))
                .andExpect(jsonPath("$.data.nationality").value("American"));
        }

        @Test
        void testGetAuthorNotFound() throws Exception {
            mockMvc.perform(get("/api/catalog/authors/{id}", "nonexistent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTHOR_NOT_FOUND"));
        }
    }

    @Nested
    class GetAllAuthors {

        @Test
        void testGetAllAuthors() throws Exception {
            long countBefore = authorRepository.count();
            createAndSaveAuthor(
                "Eric Evans", "Author of DDD",
                LocalDate.of(1957, 1, 1), null, "American"
            );
            createAndSaveAuthor(
                "Martin Fowler", "Author of Refactoring",
                LocalDate.of(1963, 1, 1), null, "British"
            );

            mockMvc.perform(get("/api/catalog/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value((int) countBefore + 2));
        }
    }

    @Nested
    class UpdateAuthor {

        @Test
        void testUpdateAuthor() throws Exception {
            Author author = createAndSaveAuthor(
                "Eric Evans", "Author of DDD",
                LocalDate.of(1957, 1, 1), null, "American"
            );
            String authorId = author.getId().getValue();

            Map<String, Object> request = Map.of(
                "name", "Eric J. Evans",
                "nationality", "USA"
            );

            mockMvc.perform(put("/api/catalog/authors/{id}", authorId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Eric J. Evans"))
                .andExpect(jsonPath("$.data.nationality").value("USA"));
        }
    }

    @Nested
    class DeleteAuthor {

        @Test
        void testDeleteAuthor() throws Exception {
            Author author = createAndSaveAuthor(
                "Eric Evans", "Author of DDD",
                LocalDate.of(1957, 1, 1), null, "American"
            );
            String authorId = author.getId().getValue();

            mockMvc.perform(delete("/api/catalog/authors/{id}", authorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

            assertThat(authorRepository.findById(author.getId())).isEmpty();
        }
    }

    // --- Helper methods ---

    private Author createAndSaveAuthor(String name, String biography,
                                        LocalDate birthDate, LocalDate deathDate,
                                        String nationality) {
        Author author = Author.create(name, biography, birthDate, deathDate, nationality);
        return authorRepository.save(author);
    }
}
