package com.library.circulation.functional;

import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;

@Component
public class CirculationScenarioState {
    private MvcResult mvcResult;
    private String loanId;
    private String holdId;
    private String copyId;
    private String patronId;
    private String bookId;

    public MvcResult getMvcResult() { return mvcResult; }
    public void setMvcResult(MvcResult mvcResult) { this.mvcResult = mvcResult; }
    public String getLoanId() { return loanId; }
    public void setLoanId(String loanId) { this.loanId = loanId; }
    public String getHoldId() { return holdId; }
    public void setHoldId(String holdId) { this.holdId = holdId; }
    public String getCopyId() { return copyId; }
    public void setCopyId(String copyId) { this.copyId = copyId; }
    public String getPatronId() { return patronId; }
    public void setPatronId(String patronId) { this.patronId = patronId; }
    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
}
