package com.library.inventory.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.inventory.application.command.CreateInventoryCommand;
import com.library.inventory.application.command.CreateLibraryCommand;
import com.library.inventory.application.dto.ApiResponse;
import com.library.inventory.application.dto.CopyInventoryDTO;
import com.library.inventory.application.dto.LibraryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String libraryId;
    private String bookId;

    @BeforeEach
    void setUp() throws Exception {
        bookId = java.util.UUID.randomUUID().toString();

        CreateLibraryCommand cmd = CreateLibraryCommand.builder()
            .code("LIB-" + System.nanoTime() % 10000)
            .name("Test Library")
            .city("Springfield")
            .build();

        MvcResult result = mockMvc.perform(post("/api/inventory/libraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cmd)))
            .andExpect(status().isCreated())
            .andReturn();

        String response = result.getResponse().getContentAsString();
        LibraryDTO library = objectMapper.readValue(
            objectMapper.readTree(response).get("data").toString(), LibraryDTO.class);
        libraryId = library.getId();
    }

    @Test
    void shouldCreateLibraryAndGetIt() throws Exception {
        mockMvc.perform(get("/api/inventory/libraries/" + libraryId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Test Library"));
    }

    @Test
    void shouldGetAllLibraries() throws Exception {
        mockMvc.perform(get("/api/inventory/libraries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void shouldCreateInventoryAndAddCopies() throws Exception {
        CreateInventoryCommand cmd = CreateInventoryCommand.builder()
            .bookId(bookId)
            .libraryId(libraryId)
            .initialCopyCount(3)
            .libraryCode("LIB01")
            .floor(1)
            .zone("A")
            .aisle("01")
            .shelf("B")
            .position("001")
            .createdBy("admin")
            .build();

        MvcResult result = mockMvc.perform(post("/api/inventory/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cmd)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalCopies").value(3))
            .andExpect(jsonPath("$.data.availableCopies").value(3))
            .andReturn();

        String response = result.getResponse().getContentAsString();
        CopyInventoryDTO inventory = objectMapper.readValue(
            objectMapper.readTree(response).get("data").toString(), CopyInventoryDTO.class);
        String inventoryId = inventory.getId();

        // Get inventory overview
        mockMvc.perform(get("/api/inventory/books/" + bookId + "/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].totalCopies").value(3));

        // Checkout a copy
        String copyId = inventory.getCopies().get(0).getId();
        mockMvc.perform(post("/api/inventory/copies/" + copyId + "/checkout"))
            .andExpect(status().isOk());

        // Verify available decreased
        mockMvc.perform(get("/api/inventory/inventories/" + inventoryId))
            .andExpect(jsonPath("$.data.availableCopies").value(2));

        // Return the copy
        mockMvc.perform(post("/api/inventory/copies/" + copyId + "/return"))
            .andExpect(status().isOk());

        // Verify available restored
        mockMvc.perform(get("/api/inventory/inventories/" + inventoryId))
            .andExpect(jsonPath("$.data.availableCopies").value(3));
    }

    @Test
    void shouldRejectDuplicateInventory() throws Exception {
        CreateInventoryCommand cmd = CreateInventoryCommand.builder()
            .bookId(bookId)
            .libraryId(libraryId)
            .initialCopyCount(1)
            .libraryCode("LIB01")
            .createdBy("admin")
            .build();

        mockMvc.perform(post("/api/inventory/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cmd)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/inventory/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cmd)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReportDamageAndLoss() throws Exception {
        CreateInventoryCommand cmd = CreateInventoryCommand.builder()
            .bookId(bookId)
            .libraryId(libraryId)
            .initialCopyCount(2)
            .libraryCode("LIB01")
            .createdBy("admin")
            .build();

        MvcResult result = mockMvc.perform(post("/api/inventory/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cmd)))
            .andExpect(status().isCreated())
            .andReturn();

        CopyInventoryDTO inventory = objectMapper.readValue(
            objectMapper.readTree(result.getResponse().getContentAsString()).get("data").toString(),
            CopyInventoryDTO.class);

        String copyId = inventory.getCopies().get(0).getId();

        // Report damage
        mockMvc.perform(post("/api/inventory/copies/" + copyId + "/damage")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Water damage\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/inventory/inventories/" + inventory.getId()))
            .andExpect(jsonPath("$.data.availableCopies").value(1));
    }

    @Test
    void shouldReturn404ForNonExistentLibrary() throws Exception {
        mockMvc.perform(get("/api/inventory/libraries/nonexistent"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldActivateAndDeactivateLibrary() throws Exception {
        mockMvc.perform(post("/api/inventory/libraries/" + libraryId + "/deactivate"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/inventory/libraries/" + libraryId))
            .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(post("/api/inventory/libraries/" + libraryId + "/activate"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/inventory/libraries/" + libraryId))
            .andExpect(jsonPath("$.data.active").value(true));
    }
}
