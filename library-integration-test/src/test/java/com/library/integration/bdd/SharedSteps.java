package com.library.integration.bdd;

import com.library.inventory.domain.model.Library;
import com.library.inventory.domain.repository.LibraryRepository;
import com.library.notification.domain.model.Notification;
import com.library.notification.domain.repository.NotificationRepository;
import com.library.patron.domain.model.Patron;
import com.library.patron.domain.model.enums.PatronType;
import com.library.patron.domain.repository.PatronRepository;
import com.library.shared.domain.model.PatronId;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Shared step definitions used across multiple feature files.
 * Extracted to avoid Cucumber "ambiguous step definition" errors.
 */
public class SharedSteps {

    @Autowired
    private PatronRepository patronRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private E2EScenarioState state;

    // --- Shared Given steps ---

    @Given("a patron {string} with email {string} exists")
    public void createPatron(String name, String email) {
        String[] parts = name.split(" ");
        Patron patron = Patron.create(parts[0], parts[1], email, PatronType.STUDENT);
        patron = patronRepository.save(patron);
        state.setPatronId(patron.getId().getValue());
    }

    @Given("a patron {string} with email {string} exists as faculty")
    public void createFacultyPatron(String name, String email) {
        String[] parts = name.split(" ");
        Patron patron = Patron.create(parts[0], parts[1], email, PatronType.FACULTY);
        patron = patronRepository.save(patron);
        state.setPatronId(patron.getId().getValue());
    }

    @Given("a patron {string} with email {string} exists with {int} active loan")
    public void createPatronWithLoans(String name, String email, int loans) {
        String[] parts = name.split(" ");
        Patron patron = Patron.create(parts[0], parts[1], email, PatronType.STUDENT);
        for (int i = 0; i < loans; i++) {
            patron.recordLoan();
        }
        patron = patronRepository.save(patron);
        state.setPatronId(patron.getId().getValue());
    }

    @Given("the main library with code {string} exists")
    public void createLibrary(String code) {
        Library library = Library.create(code, "Test Library " + code);
        libraryRepository.save(library);
    }

    // --- Shared Then steps ---

    @Then("the patron's current loan count should be {int}")
    public void verifyPatronLoanCount(int expected) {
        await().atMost(10, SECONDS).untilAsserted(() -> {
            Patron updated = patronRepository.findById(
                PatronId.of(state.getPatronId())
            ).orElseThrow();
            assertThat(updated.getCurrentLoans()).isEqualTo(expected);
        });
    }

    @Then("a notification should exist for the patron")
    public void verifyNotificationForPatron() {
        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId(state.getPatronId());
            assertThat(notifications).isNotEmpty();
        });
    }

    @Then("a notification should exist for recipient {string}")
    public void verifyNotificationForRecipient(String recipientId) {
        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findByRecipientId(recipientId);
            assertThat(notifications).isNotEmpty();
        });
    }
}
