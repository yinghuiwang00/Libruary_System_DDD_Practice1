package com.library.inventory.domain.service;

import com.library.inventory.domain.event.CopyAddedEvent;
import com.library.inventory.domain.event.CopyBorrowedEvent;
import com.library.inventory.domain.event.CopyDamagedEvent;
import com.library.inventory.domain.event.CopyLostEvent;
import com.library.inventory.domain.event.CopyReturnedEvent;
import com.library.inventory.domain.event.CopiesBatchAddedEvent;
import com.library.inventory.domain.event.InventoryCreatedEvent;
import com.library.inventory.domain.event.LowStockAlertEvent;
import com.library.inventory.domain.exception.*;
import com.library.inventory.domain.model.*;
import com.library.inventory.domain.model.enums.CopyStatus;
import com.library.inventory.domain.repository.BookCopyRepository;
import com.library.inventory.domain.repository.CopyInventoryRepository;
import com.library.inventory.domain.repository.LibraryRepository;
import com.library.inventory.infrastructure.messaging.InventoryDomainEventPublisher;
import com.library.shared.domain.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryManagementService {

    private final CopyInventoryRepository inventoryRepository;
    private final BookCopyRepository copyRepository;
    private final LibraryRepository libraryRepository;
    private final InventoryDomainEventPublisher eventPublisher;

    public InventoryManagementService(CopyInventoryRepository inventoryRepository,
                                      BookCopyRepository copyRepository,
                                      LibraryRepository libraryRepository,
                                      InventoryDomainEventPublisher eventPublisher) {
        this.inventoryRepository = inventoryRepository;
        this.copyRepository = copyRepository;
        this.libraryRepository = libraryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CopyInventory createInitialInventory(String bookId, String libraryId,
                                                int initialCopyCount, Location defaultLocation,
                                                String createdBy) {
        Library library = libraryRepository.findById(LibraryId.of(libraryId))
            .orElseThrow(() -> new LibraryNotFoundException(LibraryId.of(libraryId)));

        if (!library.isActive()) {
            throw new InvalidOperationException("Library is not active: " + libraryId);
        }

        if (inventoryRepository.existsByBookIdAndLibraryId(bookId, libraryId)) {
            throw new DuplicateInventoryException(BookId.of(bookId), LibraryId.of(libraryId));
        }

        CopyInventory inventory = CopyInventory.create(bookId, libraryId, library.getCode(), createdBy);

        if (initialCopyCount > 0) {
            inventory.addCopies(initialCopyCount, defaultLocation, "PURCHASE");
        }

        CopyInventory saved = inventoryRepository.save(inventory);

        eventPublisher.publish(new InventoryCreatedEvent(
            saved.getId().getValue(), bookId, libraryId, initialCopyCount));

        return saved;
    }

    @Transactional
    public BookCopy addCopyToInventory(String inventoryId, Location location,
                                       String acquisitionMethod, BigDecimal cost) {
        CopyInventory inventory = inventoryRepository.findById(CopyInventoryId.of(inventoryId))
            .orElseThrow(() -> new InventoryNotFoundException(CopyInventoryId.of(inventoryId)));

        String barcode = generateUniqueBarcode(inventory);
        BookCopy copy = inventory.addCopy(barcode, location, acquisitionMethod);

        if (cost != null) {
            copy.setAcquisitionCost(cost);
        }

        inventoryRepository.save(inventory);

        eventPublisher.publish(new CopyAddedEvent(
            copy.getId().getValue(), inventoryId, inventory.getBookId(), barcode));

        if (inventory.isLowStock()) {
            eventPublisher.publish(new LowStockAlertEvent(
                inventoryId, inventory.getBookId(),
                inventory.getAvailableCopies(), 2));
        }

        return copy;
    }

    @Transactional
    public List<BookCopy> batchAddCopies(String inventoryId, int count,
                                         Location location, String acquisitionMethod,
                                         BigDecimal cost) {
        CopyInventory inventory = inventoryRepository.findById(CopyInventoryId.of(inventoryId))
            .orElseThrow(() -> new InventoryNotFoundException(CopyInventoryId.of(inventoryId)));

        List<BookCopy> copies = inventory.addCopies(count, location, acquisitionMethod);

        if (cost != null) {
            copies.forEach(copy -> copy.setAcquisitionCost(cost));
        }

        inventoryRepository.save(inventory);

        eventPublisher.publish(new CopiesBatchAddedEvent(
            inventoryId, inventory.getBookId(), count));

        if (inventory.isLowStock()) {
            eventPublisher.publish(new LowStockAlertEvent(
                inventoryId, inventory.getBookId(),
                inventory.getAvailableCopies(), 2));
        }

        return copies;
    }

    @Transactional
    public void checkoutCopy(String copyId) {
        BookCopy copy = copyRepository.findById(CopyId.of(copyId))
            .orElseThrow(() -> new CopyNotFoundException(CopyId.of(copyId)));

        CopyStatus oldStatus = copy.getStatus();
        copy.markAsBorrowed();

        CopyInventory inventory = inventoryRepository.findById(CopyInventoryId.of(copy.getInventoryId()))
            .orElseThrow(() -> new InventoryNotFoundException(CopyInventoryId.of(copy.getInventoryId())));

        inventory.onCopyStatusChanged(oldStatus, copy.getStatus());
        inventoryRepository.save(inventory);
        copyRepository.save(copy);

        eventPublisher.publish(new CopyBorrowedEvent(
            copyId, inventory.getId().getValue(), inventory.getBookId(), inventory.getLibraryId()));
    }

    @Transactional
    public void returnCopy(String copyId) {
        BookCopy copy = copyRepository.findById(CopyId.of(copyId))
            .orElseThrow(() -> new CopyNotFoundException(CopyId.of(copyId)));

        CopyStatus oldStatus = copy.getStatus();
        copy.markAsReturned();

        CopyInventory inventory = inventoryRepository.findById(CopyInventoryId.of(copy.getInventoryId()))
            .orElseThrow(() -> new InventoryNotFoundException(CopyInventoryId.of(copy.getInventoryId())));

        inventory.onCopyStatusChanged(oldStatus, copy.getStatus());
        inventoryRepository.save(inventory);
        copyRepository.save(copy);

        eventPublisher.publish(new CopyReturnedEvent(
            copyId, inventory.getId().getValue(), inventory.getBookId(), inventory.getLibraryId()));
    }

    @Transactional
    public void reportCopyDamage(String copyId, String damageDescription) {
        BookCopy copy = copyRepository.findById(CopyId.of(copyId))
            .orElseThrow(() -> new CopyNotFoundException(CopyId.of(copyId)));

        CopyStatus oldStatus = copy.getStatus();
        copy.markAsDamaged(damageDescription);

        CopyInventory inventory = inventoryRepository.findById(CopyInventoryId.of(copy.getInventoryId()))
            .orElseThrow(() -> new InventoryNotFoundException(CopyInventoryId.of(copy.getInventoryId())));

        inventory.onCopyStatusChanged(oldStatus, copy.getStatus());
        inventoryRepository.save(inventory);
        copyRepository.save(copy);

        eventPublisher.publish(new CopyDamagedEvent(
            copyId, inventory.getId().getValue(), damageDescription));
    }

    @Transactional
    public void reportCopyLoss(String copyId, String reason) {
        BookCopy copy = copyRepository.findById(CopyId.of(copyId))
            .orElseThrow(() -> new CopyNotFoundException(CopyId.of(copyId)));

        CopyStatus oldStatus = copy.getStatus();
        copy.markAsLost(reason);

        CopyInventory inventory = inventoryRepository.findById(CopyInventoryId.of(copy.getInventoryId()))
            .orElseThrow(() -> new InventoryNotFoundException(CopyInventoryId.of(copy.getInventoryId())));

        inventory.onCopyStatusChanged(oldStatus, copy.getStatus());
        inventoryRepository.save(inventory);
        copyRepository.save(copy);

        eventPublisher.publish(new CopyLostEvent(
            copyId, inventory.getId().getValue(), reason));
    }

    public List<CopyInventory> getInventoryOverview(String bookId) {
        return inventoryRepository.findByBookId(bookId);
    }

    public BookCopy findAvailableCopy(String bookId, String libraryId) {
        CopyInventory inventory = inventoryRepository
            .findByBookIdAndLibraryId(bookId, libraryId)
            .orElseThrow(() -> new InventoryNotFoundException(CopyInventoryId.of("book=" + bookId + ",library=" + libraryId)));

        return inventory.getAvailableCopy();
    }

    private String generateUniqueBarcode(CopyInventory inventory) {
        return String.format("%s-%s-%06d",
            inventory.getLibraryCode(),
            inventory.getBookId().substring(0, Math.min(8, inventory.getBookId().length())),
            inventory.getTotalCopies() + 1);
    }

    public java.util.Optional<Library> findLibraryByCode(String code) {
        return libraryRepository.findByCode(code);
    }
}
