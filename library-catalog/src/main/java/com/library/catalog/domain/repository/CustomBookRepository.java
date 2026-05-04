package com.library.catalog.domain.repository;

import com.library.catalog.application.query.BookSearchCriteria;
import com.library.catalog.domain.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomBookRepository {
    Page<Book> search(BookSearchCriteria criteria, Pageable pageable);
}
