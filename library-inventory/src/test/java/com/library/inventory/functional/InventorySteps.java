package com.library.inventory.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.inventory.application.command.CreateInventoryCommand;
import com.library.inventory.application.dto.ApiResponse;
import com.library.inventory.application.dto.BookCopyDTO;
import com.library.inventory.application.dto.CopyInventoryDTO;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class InventorySteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestScenarioState state;

    @Given("^该图书在该分馆尚无馆藏记录$")
    public void noInventoryForBook() {
        // Clean state ensures this
    }

    @When("^我为图书\"([^\"]*)\"在该分馆创建馆藏，初始副本数为(\\d+)$")
    public void createInventory(String bookId, int initialCopies) throws Exception {
        CreateInventoryCommand command = CreateInventoryCommand.builder()
            .bookId(bookId)
            .libraryId(state.getLibraryId())
            .libraryCode("LIB-001")
            .initialCopyCount(initialCopies)
            .floor(1)
            .zone("A")
            .build();
        state.setMvcResult(mockMvc.perform(post("/api/inventory/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andReturn());

        ApiResponse<CopyInventoryDTO> response = readInventoryResponse(state.getMvcResult().getResponse().getContentAsString(StandardCharsets.UTF_8));
        if (response.getData() != null) {
            state.setInventoryId(response.getData().getId());
        }
    }

    @Then("馆藏创建成功")
    public void inventoryCreated() {
        assertThat(state.getMvcResult().getResponse().getStatus()).isEqualTo(201);
    }

    @Then("^总副本数为(\\d+)$")
    public void totalCopiesIs(int expected) throws Exception {
        ApiResponse<CopyInventoryDTO> response = readInventoryResponse(
            state.getMvcResult().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(response.getData().getTotalCopies()).isEqualTo(expected);
    }

    @Then("^可用副本数为(\\d+)$")
    public void availableCopiesIs(int expected) throws Exception {
        ApiResponse<CopyInventoryDTO> response = readInventoryResponse(
            state.getMvcResult().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(response.getData().getAvailableCopies()).isEqualTo(expected);
    }

    @Given("^图书\"([^\"]*)\"在该分馆有馆藏记录，包含(\\d+)个可用副本$")
    public void inventoryWithCopies(String bookId, int copies) throws Exception {
        createInventory(bookId, copies);
    }

    @When("我借出一个副本")
    public void checkoutCopy() throws Exception {
        // Get current inventory to find a copy ID
        ApiResponse<CopyInventoryDTO> invResponse = fetchInventory();
        CopyInventoryDTO inventory = invResponse.getData();

        // Find first available copy
        String copyId = inventory.getCopies().stream()
            .filter(c -> "AVAILABLE".equals(c.getStatus()))
            .map(BookCopyDTO::getId)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No available copy found"));

        state.setCopyId(copyId);

        // Checkout the copy
        state.setMvcResult(mockMvc.perform(post("/api/inventory/copies/" + copyId + "/checkout"))
            .andReturn());
    }

    @Then("借出成功")
    public void checkoutSuccess() {
        assertThat(state.getMvcResult().getResponse().getStatus()).isEqualTo(200);
    }

    @Then("可用副本数变为{int}")
    public void availableCopiesBecomes(int expected) throws Exception {
        ApiResponse<CopyInventoryDTO> response = fetchInventory();
        assertThat(response.getData().getAvailableCopies()).isEqualTo(expected);
    }

    @When("我归还该副本")
    public void returnCopy() throws Exception {
        state.setMvcResult(mockMvc.perform(post("/api/inventory/copies/" + state.getCopyId() + "/return"))
            .andReturn());
    }

    @Then("归还成功")
    public void returnSuccess() {
        assertThat(state.getMvcResult().getResponse().getStatus()).isEqualTo(200);
    }

    @SuppressWarnings("unchecked")
    private ApiResponse<CopyInventoryDTO> readInventoryResponse(String json) throws Exception {
        return objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, CopyInventoryDTO.class));
    }

    @SuppressWarnings("unchecked")
    private ApiResponse<CopyInventoryDTO> fetchInventory() throws Exception {
        var result = mockMvc.perform(get("/api/inventory/inventories/" + state.getInventoryId()))
            .andReturn();
        return objectMapper.readValue(
            result.getResponse().getContentAsString(StandardCharsets.UTF_8),
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, CopyInventoryDTO.class));
    }
}
