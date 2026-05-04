package com.library.catalog.domain.service;

import com.library.catalog.domain.exception.AuthorNotFoundException;
import com.library.catalog.domain.model.Author;
import com.library.catalog.domain.repository.AuthorRepository;
import com.library.shared.domain.model.AuthorId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuthorManagementService {

    private final AuthorRepository authorRepository;

    public AuthorManagementService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Transactional
    public Author createAuthor(String name, String biography, LocalDate birthDate,
                               LocalDate deathDate, String nationality) {
        Author author = Author.create(name, biography, birthDate, deathDate, nationality);
        return authorRepository.save(author);
    }

    @Transactional
    public Author updateAuthor(AuthorId authorId, String name, String nationality,
                               LocalDate birthDate, LocalDate deathDate, String biography) {
        Author author = findAuthorOrThrow(authorId);
        author.updatePersonalInfo(name, nationality, birthDate, deathDate);
        if (biography != null) {
            author.updateBiography(biography);
        }
        return authorRepository.save(author);
    }

    public Author getAuthor(AuthorId authorId) {
        return findAuthorOrThrow(authorId);
    }

    public Page<Author> searchAuthors(String name, Pageable pageable) {
        return authorRepository.findByNameContaining(name, pageable);
    }

    public List<Author> getAllAuthors() {
        return authorRepository.findAll();
    }

    @Transactional
    public void deleteAuthor(AuthorId authorId) {
        if (!authorRepository.existsById(authorId)) {
            throw new AuthorNotFoundException("Author not found: " + authorId.getValue());
        }
        authorRepository.deleteById(authorId);
    }

    private Author findAuthorOrThrow(AuthorId authorId) {
        return authorRepository.findById(authorId)
            .orElseThrow(() -> new AuthorNotFoundException("Author not found: " + authorId.getValue()));
    }
}
