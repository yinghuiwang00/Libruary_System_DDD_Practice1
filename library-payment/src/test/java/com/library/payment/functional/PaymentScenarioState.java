package com.library.payment.functional;

import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;

@Component
public class PaymentScenarioState {
    private MvcResult mvcResult;
    private String paymentId;

    public MvcResult getMvcResult() { return mvcResult; }
    public void setMvcResult(MvcResult mvcResult) { this.mvcResult = mvcResult; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
}
