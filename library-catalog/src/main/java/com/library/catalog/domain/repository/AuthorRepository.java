package com.library.catalog.domain.repository;

import com.library.catalog.domain.model.Author;
import com.library.shared.domain.model.AuthorId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, AuthorId> {

    Optional<Author> findByName(String name);

    Page<Author> findByNameContaining(String name, Pageable pageable);

    List<Author> findByNationality(String nationality);
}
