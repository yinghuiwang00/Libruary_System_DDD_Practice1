package com.library.inventory.application.service;

import com.library.inventory.application.command.AddCopyCommand;
import com.library.inventory.application.command.BatchAddCopiesCommand;
import com.library.inventory.application.command.CreateInventoryCommand;
import com.library.inventory.application.dto.BookCopyDTO;
import com.library.inventory.application.dto.CopyInventoryDTO;
import com.library.inventory.domain.model.CopyInventory;
import com.library.inventory.domain.model.Location;
import com.library.inventory.domain.repository.CopyInventoryRepository;
import com.library.inventory.domain.service.InventoryManagementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class InventoryApplicationService {

    private final InventoryManagementService inventoryManagementService;
    private final CopyInventoryRepository inventoryRepository;

    public InventoryApplicationService(InventoryManagementService inventoryManagementService,
                                       CopyInventoryRepository inventoryRepository) {
        this.inventoryManagementService = inventoryManagementService;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public CopyInventoryDTO createInventory(CreateInventoryCommand command) {
        Location location = buildLocation(command.getLibraryCode(), command.getFloor(),
            command.getZone(), command.getAisle(), command.getShelf(), command.getPosition());

        CopyInventory inventory = inventoryManagementService.createInitialInventory(
            command.getBookId(),
            command.getLibraryId(),
            command.getInitialCopyCount(),
            location,
            command.getCreatedBy());

        return CopyInventoryDTO.fromDomain(inventory);
    }

    @Transactional
    public BookCopyDTO addCopy(AddCopyCommand command) {
        Location location = buildLocation(command.getLibraryCode(), command.getFloor(),
            command.getZone(), command.getAisle(), command.getShelf(), command.getPosition());

        return BookCopyDTO.fromDomain(
            inventoryManagementService.addCopyToInventory(
                command.getInventoryId(),
                location,
                command.getAcquisitionMethod(),
                command.getCost()));
    }

    @Transactional
    public List<BookCopyDTO> batchAddCopies(BatchAddCopiesCommand command) {
        Location location = buildLocation(command.getLibraryCode(), command.getFloor(),
            command.getZone(), command.getAisle(), command.getShelf(), command.getPosition());

        return inventoryManagementService.batchAddCopies(
                command.getInventoryId(),
                command.getCount(),
                location,
                command.getAcquisitionMethod(),
                command.getCost())
            .stream()
            .map(BookCopyDTO::fromDomain)
            .collect(Collectors.toList());
    }

    @Transactional
    public void checkoutCopy(String copyId) {
        inventoryManagementService.checkoutCopy(copyId);
    }

    @Transactional
    public void returnCopy(String copyId) {
        inventoryManagementService.returnCopy(copyId);
    }

    @Transactional
    public void reportDamage(String copyId, String description) {
        inventoryManagementService.reportCopyDamage(copyId, description);
    }

    @Transactional
    public void reportLoss(String copyId, String reason) {
        inventoryManagementService.reportCopyLoss(copyId, reason);
    }

    public CopyInventoryDTO getInventory(String inventoryId) {
        CopyInventory inventory = inventoryRepository.findById(
                com.library.shared.domain.model.CopyInventoryId.of(inventoryId))
            .orElseThrow(() -> new com.library.inventory.domain.exception.InventoryNotFoundException(
                com.library.shared.domain.model.CopyInventoryId.of(inventoryId)));
        return CopyInventoryDTO.fromDomain(inventory);
    }

    public List<CopyInventoryDTO> getInventoryOverview(String bookId) {
        return inventoryManagementService.getInventoryOverview(bookId).stream()
            .map(CopyInventoryDTO::fromDomain)
            .collect(Collectors.toList());
    }

    private Location buildLocation(String libraryCode, Integer floor, String zone,
                                   String aisle, String shelf, String position) {
        return Location.of(libraryCode, floor, zone, aisle, shelf, position);
    }
}
