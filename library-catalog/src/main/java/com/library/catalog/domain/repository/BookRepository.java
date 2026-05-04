package com.library.catalog.domain.repository;

import com.library.catalog.domain.model.Book;
import com.library.catalog.domain.model.ISBN;
import com.library.catalog.domain.model.enums.BookStatus;
import com.library.shared.domain.model.BookId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, BookId>, CustomBookRepository {

    Optional<Book> findByIsbn(ISBN isbn);

    boolean existsByIsbn(ISBN isbn);

    List<Book> findByStatus(BookStatus status);

    Page<Book> findByStatus(BookStatus status, Pageable pageable);

    Page<Book> findByTitleContaining(String title, Pageable pageable);

    @Query("SELECT b FROM Book b JOIN b.authors ba WHERE ba.authorName LIKE %:name%")
    Page<Book> findByAuthorName(@Param("name") String authorName, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.status = :status AND b.title LIKE %:title%")
    Page<Book> findByStatusAndTitle(@Param("status") BookStatus status, @Param("title") String title, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.publisherId = :publisherId")
    Page<Book> findByPublisherId(@Param("publisherId") String publisherId, Pageable pageable);

    @Query("SELECT b FROM Book b JOIN b.categoryIds c WHERE c = :categoryId")
    Page<Book> findByCategoryId(@Param("categoryId") String categoryId, Pageable pageable);
}
