package com.library.analytics.functional;

import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

@Component
public class AnalyticsScenarioState {

    private MvcResult mvcResult;
    private String currentReportId;
    private String lastReportId;
    private final List<String> createdReportIds = new ArrayList<>();

    public MvcResult getMvcResult() { return mvcResult; }
    public void setMvcResult(MvcResult mvcResult) { this.mvcResult = mvcResult; }

    public String getCurrentReportId() { return currentReportId; }
    public void setCurrentReportId(String currentReportId) {
        this.currentReportId = currentReportId;
        this.lastReportId = currentReportId;
        this.createdReportIds.add(currentReportId);
    }

    public String getLastReportId() { return lastReportId; }

    public List<String> getCreatedReportIds() { return createdReportIds; }

    public void reset() {
        this.mvcResult = null;
        this.currentReportId = null;
        this.lastReportId = null;
        this.createdReportIds.clear();
    }
}
