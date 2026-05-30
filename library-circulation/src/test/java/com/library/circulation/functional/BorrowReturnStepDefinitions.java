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

    @Given("^读者\"([^\"]*)\"想要借阅图书\"([^\"]*)\"的副本\"([^\"]*)\"$")
    public void patronWantsToBorrow(String patronId, String bookId, String copyId) {
        state.setPatronId(patronId);
        state.setBookId(bookId);
        state.setCopyId(copyId);
    }

    @When("读者借出该图书")
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

    @Then("借出成功")
    public void borrowSuccess() {
        assertThat(state.getMvcResult().getResponse().getStatus()).isEqualTo(201);
    }

    @Then("^借阅状态为\"([^\"]*)\"$")
    public void loanStatusIs(String expectedStatus) throws Exception {
        ApiResponse<LoanDTO> response = readLoanResponse(state.getMvcResult());
        assertThat(response.getData().getStatus()).isEqualTo(expectedStatus);
    }

    // ========== Return steps ==========

    @Given("^读者\"([^\"]*)\"已经借出了图书\"([^\"]*)\"的副本\"([^\"]*)\"$")
    public void patronAlreadyBorrowed(String patronId, String bookId, String copyId) throws Exception {
        state.setPatronId(patronId);
        state.setBookId(bookId);
        state.setCopyId(copyId);
        borrowBook();
    }

    @When("读者归还该图书")
    public void returnBook() throws Exception {
        state.setMvcResult(mockMvc.perform(post("/api/circulation/loans/" + state.getLoanId() + "/return"))
            .andReturn());
    }

    @Then("归还成功")
    public void returnSuccess() {
        assertThat(state.getMvcResult().getResponse().getStatus()).isEqualTo(200);
    }

    // ========== Hold steps ==========

    @Given("^图书\"([^\"]*)\"当前不可借阅$")
    public void bookNotAvailable(String bookId) {
        state.setBookId(bookId);
    }

    @When("^读者\"([^\"]*)\"预约该图书$")
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

    @Then("预约成功")
    public void holdSuccess() {
        assertThat(state.getMvcResult().getResponse().getStatus()).isEqualTo(201);
    }

    @Then("^预约状态为\"([^\"]*)\"$")
    public void holdStatusIs(String expectedStatus) throws Exception {
        ApiResponse<HoldDTO> response = readHoldResponse(state.getMvcResult());
        assertThat(response.getData().getStatus()).isEqualTo(expectedStatus);
    }

    // ========== Renew steps ==========

    @When("读者续借该图书")
    public void renewBook() throws Exception {
        state.setMvcResult(mockMvc.perform(post("/api/circulation/loans/" + state.getLoanId() + "/renew"))
            .andReturn());

        ApiResponse<LoanDTO> response = readLoanResponse(state.getMvcResult());
        if (response.getData() != null) {
            state.setMvcResult(state.getMvcResult());
        }
    }

    @Then("续借成功")
    public void renewSuccess() {
        assertThat(state.getMvcResult().getResponse().getStatus()).isEqualTo(200);
    }

    @Then("^续借次数为(\\d+)$")
    public void renewalCountIs(int expectedCount) throws Exception {
        ApiResponse<LoanDTO> response = readLoanResponse(state.getMvcResult());
        assertThat(response.getData().getRenewalCount()).isEqualTo(expectedCount);
    }

    @Then("应还日期已延长")
    public void dueDateExtended() throws Exception {
        ApiResponse<LoanDTO> response = readLoanResponse(state.getMvcResult());
        assertThat(response.getData().getDueDate()).isAfter(state.getOriginalDueDate());
    }

    // ========== Renewal limit steps ==========

    @Given("^读者\"([^\"]*)\"已经借出了图书\"([^\"]*)\"的副本\"([^\"]*)\"且已达到最大续借次数$")
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

    @When("读者尝试续借该图书")
    public void tryRenewBook() throws Exception {
        state.setMvcResult(mockMvc.perform(post("/api/circulation/loans/" + state.getLoanId() + "/renew"))
            .andReturn());
    }

    @Then("续借应该失败")
    public void renewShouldFail() {
        int status = state.getMvcResult().getResponse().getStatus();
        assertThat(status).isEqualTo(409);
    }

    // ========== Overdue steps ==========

    @Given("^读者\"([^\"]*)\"已经借出了图书\"([^\"]*)\"的副本\"([^\"]*)\"且已逾期$")
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

    @When("系统处理逾期借阅")
    public void systemProcessesOverdueLoans() throws Exception {
        state.setMvcResult(mockMvc.perform(post("/api/circulation/admin/process-overdue"))
            .andReturn());
    }

    @Then("^借阅状态应该为\"([^\"]*)\"$")
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

    @Given("^读者\"([^\"]*)\"已经预约了图书\"([^\"]*)\"$")
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

    @When("读者取消该预约")
    public void cancelHold() throws Exception {
        state.setMvcResult(mockMvc.perform(
                delete("/api/circulation/holds/" + state.getHoldId())
                    .param("reason", "读者主动取消"))
            .andReturn());
    }

    @Then("预约已取消")
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
