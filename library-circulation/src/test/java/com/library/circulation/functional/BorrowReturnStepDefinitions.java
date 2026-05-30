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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class BorrowReturnStepDefinitions {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CirculationScenarioState state;

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
