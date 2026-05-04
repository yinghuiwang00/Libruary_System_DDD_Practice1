package com.library.catalog.domain.repository;

import com.library.catalog.domain.model.Publisher;
import com.library.shared.domain.model.PublisherId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PublisherRepository extends JpaRepository<Publisher, PublisherId> {

    Optional<Publisher> findByName(String name);

    Page<Publisher> findByNameContaining(String name, Pageable pageable);
}
