package com.library.patron.functional;

import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

@Component
public class PatronScenarioState {
    private MvcResult mvcResult;
    private String patronId;
    private LocalDate membershipExpiryBefore;

    public MvcResult getMvcResult() { return mvcResult; }
    public void setMvcResult(MvcResult mvcResult) { this.mvcResult = mvcResult; }
    public String getPatronId() { return patronId; }
    public void setPatronId(String patronId) { this.patronId = patronId; }
    public LocalDate getMembershipExpiryBefore() { return membershipExpiryBefore; }
    public void setMembershipExpiryBefore(LocalDate membershipExpiryBefore) { this.membershipExpiryBefore = membershipExpiryBefore; }
}
