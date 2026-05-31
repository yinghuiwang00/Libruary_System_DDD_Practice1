package com.library.circulation.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.circulation.application.command.BorrowBookCommand;
import com.library.circulation.application.command.PlaceHoldCommand;
import com.library.circulation.application.dto.ApiResponse;
import com.library.circulation.application.dto.HoldDTO;
import com.library.circulation.application.dto.LoanDTO;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

public class BorrowReturnStepDefinitions {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CirculationScenarioState state;

    // ========== Borrow steps ==========

    @Given("^patron \"([^\"]*)\" wants to borrow copy \"([^\"]*)\" of book \"([^\"]*)\"$")
    public void patronWantsToBorrow(String patronId, String bookId, String copyId) {
        state.setPatronId(patronId);
        state.setBookId(bookId);
        state.setCopyId(copyId);
    }

    @When("the patron borrows the book")
    public void borrowBook() throws Exception {
        BorrowBookCommand command = new BorrowBookCommand(
            state.getCopyId(), state.getPatronId(), state.getBookId()
        );
        state.setMvcResult(mockMvc.perform(post("/api/circulation/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andReturn());

        ApiResponse<LoanDTO> response = readLoanResponse(state.getMvcResult());
        if (response.getData() != null) {
            state.setLoanId(response.getData().getId());
            state.setOriginalDueDate(response.getData().getDueDate());
        }
    }

    @Then("the borrowing succeeds")
    public void borrowSuccess() {
        assertThat(state.getMvcResult().getResponse().getStatus()).isEqualTo(201);
    }

    @Then("^the loan status is \"([^\"]*)\"$")
    public void loanStatusIs(String expectedStatus) throws Exception {
        ApiResponse<LoanDTO> response = readLoanResponse(state.getMvcResult());
        assertThat(response.getData().getStatus()).isEqualTo(expectedStatus);
    }

    // ========== Return steps ==========

    @Given("^patron \"([^\"]*)\" has already borrowed copy \"([^\"]*)\" of book \"([^\"]*)\"$")
    public void patronAlreadyBorrowed(String patronId, String bookId, String copyId) throws Exception {
        state.setPatronId(patronId);
        state.setBookId(bookId);
        state.setCopyId(copyId);
        borrowBook();
    }

    @When("the patron returns the book")
    public void returnBook() throws Exception {
        state.setMvcResult(mockMvc.perform(post("/api/circulation/loans/" + state.getLoanId() + "/return"))
            .andReturn());
    }

    @Then("the return succeeds")
    public void returnSuccess() {
        assertThat(state.getMvcResult().getResponse().getStatus()).isEqualTo(200);
    }

    // ========== Hold steps ==========

    @Given("^book \"([^\"]*)\" is currently unavailable$")
    public void bookNotAvailable(String bookId) {
        state.setBookId(bookId);
    }

    @When("^patron \"([^\"]*)\" places a hold on the book$")
    public void placeHold(String patronId) throws Exception {
        state.setPatronId(patronId);
        PlaceHoldCommand command = new PlaceHoldCommand(
            state.getBookId(), patronId, "LIB-001"
        );
        state.setMvcResult(mockMvc.perform(post("/api/circulation/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andReturn());

        ApiResponse<HoldDTO> response = readHoldResponse(state.getMvcResult());
        if (response.getData() != null) {
            state.setHoldId(response.getData().getId());
        }
    }

    @Then("the hold is placed successfully")
    public void holdSuccess() {
        assertThat(state.getMvcResult().getResponse().getStatus()).isEqualTo(201);
    }

    @Then("^the hold status is \"([^\"]*)\"$")
    public void holdStatusIs(String expectedStatus) throws Exception {
        ApiResponse<HoldDTO> response = readHoldResponse(state.getMvcResult());
        assertThat(response.getData().getStatus()).isEqualTo(expectedStatus);
    }

    // ========== Renew steps ==========

    @When("the patron renews the book")
    public void renewBook() throws Exception {
        state.setMvcResult(mockMvc.perform(post("/api/circulation/loans/" + state.getLoanId() + "/renew"))
            .andReturn());

        ApiResponse<LoanDTO> response = readLoanResponse(state.getMvcResult());
        if (response.getData() != null) {
            state.setMvcResult(state.getMvcResult());
        }
    }

    @Then("the renewal succeeds")
    public void renewSuccess() {
        assertThat(state.getMvcResult().getResponse().getStatus()).isEqualTo(200);
    }

    @Then("^the renewal count is (\\d+)$")
    public void renewalCountIs(int expectedCount) throws Exception {
        ApiResponse<LoanDTO> response = readLoanResponse(state.getMvcResult());
        assertThat(response.getData().getRenewalCount()).isEqualTo(expectedCount);
    }

    @Then("the due date has been extended")
    public void dueDateExtended() throws Exception {
        ApiResponse<LoanDTO> response = readLoanResponse(state.getMvcResult());
        assertThat(response.getData().getDueDate()).isAfter(state.getOriginalDueDate());
    }

    // ========== Renewal limit steps ==========

    @Given("^patron \"([^\"]*)\" has already borrowed copy \"([^\"]*)\" of book \"([^\"]*)\" and reached the maximum renewal count$")
    public void patronAlreadyBorrowedWithMaxRenewals(String patronId, String bookId, String copyId) throws Exception {
        state.setPatronId(patronId);
        state.setBookId(bookId);
        state.setCopyId(copyId);
        borrowBook();

        // Renew up to max (2 renewals for standard policy)
        for (int i = 0; i < 2; i++) {
            MvcResult renewResult = mockMvc.perform(
                post("/api/circulation/loans/" + state.getLoanId() + "/renew")
            ).andReturn();
            assertThat(renewResult.getResponse().getStatus()).isEqualTo(200);
        }
    }

    @When("the patron attempts to renew the book")
    public void tryRenewBook() throws Exception {
        state.setMvcResult(mockMvc.perform(post("/api/circulation/loans/" + state.getLoanId() + "/renew"))
            .andReturn());
    }

    @Then("the renewal should fail")
    public void renewShouldFail() {
        int status = state.getMvcResult().getResponse().getStatus();
        assertThat(status).isEqualTo(409);
    }

    // ========== Overdue steps ==========

    @Given("^patron \"([^\"]*)\" has already borrowed copy \"([^\"]*)\" of book \"([^\"]*)\" and it is overdue$")
    public void patronAlreadyBorrowedAndOverdue(String patronId, String bookId, String copyId) throws Exception {
        state.setPatronId(patronId);
        state.setBookId(bookId);
        state.setCopyId(copyId);
        borrowBook();

        // Update the loan's due date to the past to simulate overdue
        // We use JDBC to directly set the due_date in the past
        java.time.LocalDateTime pastDate = java.time.LocalDateTime.now().minusDays(5);
        state.getJdbcTemplate().update(
            "UPDATE loans SET due_date = ? WHERE id = ?",
            pastDate, state.getLoanId()
        );
    }

    @When("the system processes overdue loans")
    public void systemProcessesOverdueLoans() throws Exception {
        state.setMvcResult(mockMvc.perform(post("/api/circulation/admin/process-overdue"))
            .andReturn());
    }

    @Then("^the loan status should be \"([^\"]*)\"$")
    public void loanStatusShouldBe(String expectedStatus) throws Exception {
        // The process-overdue returns a list of overdue loans
        String json = state.getMvcResult().getResponse().getContentAsString(StandardCharsets.UTF_8);
        ApiResponse<List<LoanDTO>> response = objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(
                ApiResponse.class,
                objectMapper.getTypeFactory().constructParametricType(List.class, LoanDTO.class)
            ));

        assertThat(response.getData()).isNotEmpty();
        boolean found = response.getData().stream()
            .anyMatch(loan -> loan.getId().equals(state.getLoanId())
                && loan.getStatus().equals(expectedStatus));
        assertThat(found).isTrue();
    }

    // ========== Cancel hold steps ==========

    @Given("^patron \"([^\"]*)\" has already placed a hold on book \"([^\"]*)\"$")
    public void patronAlreadyPlacedHold(String patronId, String bookId) throws Exception {
        state.setBookId(bookId);
        state.setPatronId(patronId);
        PlaceHoldCommand command = new PlaceHoldCommand(
            bookId, patronId, "LIB-001"
        );
        MvcResult result = mockMvc.perform(post("/api/circulation/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andReturn();

        ApiResponse<HoldDTO> response = readHoldResponse(result);
        if (response.getData() != null) {
            state.setHoldId(response.getData().getId());
        }
    }

    @When("the patron cancels the hold")
    public void cancelHold() throws Exception {
        state.setMvcResult(mockMvc.perform(
                delete("/api/circulation/holds/" + state.getHoldId())
                    .param("reason", "读者主动取消"))
            .andReturn());
    }

    @Then("the hold is cancelled")
    public void holdCancelled() {
        assertThat(state.getMvcResult().getResponse().getStatus()).isEqualTo(200);
    }

    // ========== Helper methods ==========

    @SuppressWarnings("unchecked")
    private ApiResponse<LoanDTO> readLoanResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, LoanDTO.class));
    }

    @SuppressWarnings("unchecked")
    private ApiResponse<HoldDTO> readHoldResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, HoldDTO.class));
    }
}
