package com.library.inventory.domain.repository;

import com.library.inventory.domain.model.CopyInventory;
import com.library.shared.domain.model.BookId;
import com.library.shared.domain.model.CopyInventoryId;
import com.library.shared.domain.model.LibraryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CopyInventoryRepository extends JpaRepository<CopyInventory, CopyInventoryId> {

    Optional<CopyInventory> findByBookIdAndLibraryId(String bookId, String libraryId);

    List<CopyInventory> findByBookId(String bookId);

    List<CopyInventory> findByLibraryId(String libraryId);

    List<CopyInventory> findByBookIdAndAvailableCopiesGreaterThan(String bookId, int minAvailable);

    boolean existsByBookIdAndLibraryId(String bookId, String libraryId);

    List<CopyInventory> findByAvailableCopiesLessThanEqual(int threshold);
}
