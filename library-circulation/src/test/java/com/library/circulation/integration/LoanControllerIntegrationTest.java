package com.library.circulation.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.circulation.application.command.BorrowBookCommand;
import com.library.circulation.application.dto.ApiResponse;
import com.library.circulation.application.dto.LoanDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoanControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldBorrowBookSuccessfully() throws Exception {
        BorrowBookCommand command = new BorrowBookCommand(
            java.util.UUID.randomUUID().toString(),
            java.util.UUID.randomUUID().toString(),
            java.util.UUID.randomUUID().toString()
        );

        MvcResult result = mockMvc.perform(post("/api/circulation/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.copyId").value(command.getCopyId()))
            .andExpect(jsonPath("$.data.patronId").value(command.getPatronId()))
            .andExpect(jsonPath("$.data.bookId").value(command.getBookId()))
            .andReturn();

        ApiResponse<LoanDTO> response = readLoanResponse(result);
        assertThat(response.getData().getDueDate()).isNotNull();
        assertThat(response.getData().getLoanDate()).isNotNull();
    }

    @Test
    void shouldReturnBookSuccessfully() throws Exception {
        String loanId = createBorrowedLoan();

        mockMvc.perform(post("/api/circulation/loans/" + loanId + "/return"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("RETURNED"))
            .andExpect(jsonPath("$.data.id").value(loanId));
    }

    @Test
    void shouldRenewLoanSuccessfully() throws Exception {
        String loanId = createBorrowedLoan();

        mockMvc.perform(post("/api/circulation/loans/" + loanId + "/renew"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("RENEWED"))
            .andExpect(jsonPath("$.data.renewalCount").value(1));
    }

    @Test
    void shouldGetLoanById() throws Exception {
        String loanId = createBorrowedLoan();

        mockMvc.perform(get("/api/circulation/loans/" + loanId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(loanId))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void shouldGetPatronLoans() throws Exception {
        String patronId = java.util.UUID.randomUUID().toString();
        BorrowBookCommand command = new BorrowBookCommand(
            java.util.UUID.randomUUID().toString(),
            patronId,
            java.util.UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/api/circulation/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/circulation/patrons/" + patronId + "/loans"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].patronId").value(patronId));
    }

    private String createBorrowedLoan() throws Exception {
        BorrowBookCommand command = new BorrowBookCommand(
            java.util.UUID.randomUUID().toString(),
            java.util.UUID.randomUUID().toString(),
            java.util.UUID.randomUUID().toString()
        );

        MvcResult result = mockMvc.perform(post("/api/circulation/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andExpect(status().isCreated())
            .andReturn();

        ApiResponse<LoanDTO> response = readLoanResponse(result);
        return response.getData().getId();
    }

    @SuppressWarnings("unchecked")
    private ApiResponse<LoanDTO> readLoanResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        return objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, LoanDTO.class));
    }
}
