package com.library.inventory.domain.repository;

import com.library.inventory.domain.model.Library;
import com.library.shared.domain.model.LibraryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LibraryRepository extends JpaRepository<Library, LibraryId> {

    Optional<Library> findByCode(String code);

    List<Library> findByActiveTrue();

    boolean existsByCode(String code);
}
