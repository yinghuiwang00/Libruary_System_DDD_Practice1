package com.library.inventory.functional;

import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;

@Component
public class TestScenarioState {
    private MvcResult mvcResult;
    private String libraryId;
    private String inventoryId;
    private String copyId;

    public MvcResult getMvcResult() { return mvcResult; }
    public void setMvcResult(MvcResult mvcResult) { this.mvcResult = mvcResult; }
    public String getLibraryId() { return libraryId; }
    public void setLibraryId(String libraryId) { this.libraryId = libraryId; }
    public String getInventoryId() { return inventoryId; }
    public void setInventoryId(String inventoryId) { this.inventoryId = inventoryId; }
    public String getCopyId() { return copyId; }
    public void setCopyId(String copyId) { this.copyId = copyId; }
}
