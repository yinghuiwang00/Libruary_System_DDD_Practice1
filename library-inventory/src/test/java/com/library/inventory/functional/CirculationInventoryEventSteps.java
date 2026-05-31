package com.library.inventory.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.inventory.application.handler.BookBorrowedInventoryHandler;
import com.library.inventory.application.handler.BookReturnedInventoryHandler;
import com.library.inventory.domain.model.*;
import com.library.inventory.domain.model.enums.CopyStatus;
import com.library.inventory.domain.repository.BookCopyRepository;
import com.library.inventory.domain.repository.CopyInventoryRepository;
import com.library.inventory.domain.repository.LibraryRepository;
import com.library.inventory.domain.service.InventoryManagementService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class CirculationInventoryEventSteps {

    @Autowired
    private BookBorrowedInventoryHandler borrowedHandler;

    @Autowired
    private BookReturnedInventoryHandler returnedHandler;

    @Autowired
    private InventoryManagementService inventoryService;

    @Autowired
    private CopyInventoryRepository inventoryRepository;

    @Autowired
    private BookCopyRepository copyRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private BookCopy testCopy;

    @Given("book {string} has 1 available copy in that library")
    public void inventoryWithOneAvailableCopy(String bookId) {
        Library library = libraryRepository.findByCode("MAIN-LIB-001")
            .orElseThrow(() -> new AssertionError("Library MAIN-LIB-001 should exist"));

        CopyInventory inventory = inventoryService.createInitialInventory(
            bookId, library.getId().getValue(), 1, Location.simple("MAIN", "DEFAULT"), "SYSTEM"
        );
        testCopy = inventory.getCopies().get(0);
    }

    @Given("book {string} has 1 borrowed copy in that library")
    public void inventoryWithOneBorrowedCopy(String bookId) {
        Library library = libraryRepository.findByCode("MAIN-LIB-001")
            .orElseThrow(() -> new AssertionError("Library MAIN-LIB-001 should exist"));

        CopyInventory inventory = inventoryService.createInitialInventory(
            bookId, library.getId().getValue(), 1, Location.simple("MAIN", "DEFAULT"), "SYSTEM"
        );
        testCopy = inventory.getCopies().get(0);

        // Mark as borrowed via service to keep @Version consistent
        inventoryService.checkoutCopy(testCopy.getId().getValue());
    }

    @When("the module receives a borrow event for that copy")
    public void receiveBookBorrowedEvent() throws Exception {
        String copyId = testCopy.getId().getValue();
        String eventJson = """
            {
                "eventType": "BookBorrowedEvent",
                "eventId": "evt-borrow-bdd-001",
                "copyId": {"value": "%s"},
                "bookId": {"value": "BOOK-CIRC-100"},
                "loanId": {"value": "LOAN-BDD-001"},
                "patronId": {"value": "PATRON-BDD-001"}
            }
            """.formatted(copyId);
        JsonNode event = objectMapper.readTree(eventJson);
        borrowedHandler.handle(event);
    }

    @When("the module receives a return event for that copy")
    public void receiveBookReturnedEvent() throws Exception {
        String copyId = testCopy.getId().getValue();
        String eventJson = """
            {
                "eventType": "BookReturnedEvent",
                "eventId": "evt-return-bdd-001",
                "copyId": {"value": "%s"},
                "bookId": {"value": "BOOK-CIRC-200"},
                "loanId": {"value": "LOAN-BDD-002"},
                "patronId": {"value": "PATRON-BDD-001"}
            }
            """.formatted(copyId);
        JsonNode event = objectMapper.readTree(eventJson);
        returnedHandler.handle(event);
    }

    @Then("the copy's status should become {string}")
    public void verifyCopyStatus(String expectedStatus) {
        BookCopy updated = copyRepository.findById(testCopy.getId()).orElseThrow();
        assertThat(updated.getStatus().name()).isEqualTo(expectedStatus);
    }
}
