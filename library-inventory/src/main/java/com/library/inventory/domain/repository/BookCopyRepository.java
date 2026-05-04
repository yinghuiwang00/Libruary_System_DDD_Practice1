package com.library.inventory.domain.repository;

import com.library.inventory.domain.model.BookCopy;
import com.library.inventory.domain.model.enums.CopyStatus;
import com.library.shared.domain.model.CopyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, CopyId> {

    Optional<BookCopy> findByBarcode(String barcode);

    List<BookCopy> findByInventoryId(String inventoryId);

    List<BookCopy> findByStatus(CopyStatus status);

    List<BookCopy> findByStatusIn(List<CopyStatus> statuses);

    boolean existsByBarcode(String barcode);
}
