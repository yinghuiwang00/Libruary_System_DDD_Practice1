package com.library.catalog.functional;

import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;

@Component
public class TestScenarioState {

    private MvcResult mvcResult;
    private String bookId;

    public MvcResult getMvcResult() {
        return mvcResult;
    }

    public void setMvcResult(MvcResult mvcResult) {
        this.mvcResult = mvcResult;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }
}
