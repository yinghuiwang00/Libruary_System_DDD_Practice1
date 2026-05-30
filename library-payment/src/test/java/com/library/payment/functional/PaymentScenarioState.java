package com.library.payment.functional;

import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

@Component
public class PaymentScenarioState {
    private MvcResult mvcResult;
    private String paymentId;
    private String refundId;
    private final List<String> paymentIds = new ArrayList<>();

    public MvcResult getMvcResult() { return mvcResult; }
    public void setMvcResult(MvcResult mvcResult) { this.mvcResult = mvcResult; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getRefundId() { return refundId; }
    public void setRefundId(String refundId) { this.refundId = refundId; }
    public List<String> getPaymentIds() { return paymentIds; }
    public void addPaymentId(String paymentId) {
        this.paymentId = paymentId;
        this.paymentIds.add(paymentId);
    }
}
