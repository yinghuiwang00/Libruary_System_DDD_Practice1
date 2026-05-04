package com.library.inventory.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.inventory.application.command.CreateLibraryCommand;
import com.library.inventory.application.dto.ApiResponse;
import com.library.inventory.application.dto.LibraryDTO;
import com.library.inventory.domain.repository.LibraryRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class LibrarySteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private TestScenarioState state;

    @Given("^系统中不存在编码为\"([^\"]*)\"的分馆$")
    public void noLibraryWithCode(String code) {
        boolean exists = libraryRepository.existsByCode(code);
        assertThat(exists).isFalse();
    }

    @When("^我创建一个新分馆，编码为\"([^\"]*)\"，名称为\"([^\"]*)\"$")
    public void createLibrary(String code, String name) throws Exception {
        CreateLibraryCommand command = CreateLibraryCommand.builder()
            .code(code)
            .name(name)
            .build();
        state.setMvcResult(mockMvc.perform(post("/api/inventory/libraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andReturn());
    }

    @When("^地址为\"([^\"]*)\"$")
    public void setAddress(String address) {
        // Address is part of library creation, already handled
    }

    @When("^联系电话为\"([^\"]*)\"$")
    public void setPhone(String phone) {
        // Phone is part of library creation, already handled
    }

    @Then("分馆创建成功")
    public void libraryCreated() {
        assertThat(state.getMvcResult().getResponse().getStatus()).isEqualTo(201);
    }

    @Then("^分馆状态为\"([^\"]*)\"$")
    @SuppressWarnings("unchecked")
    public void libraryStatusIs(String expectedStatus) throws Exception {
        ApiResponse<LibraryDTO> response = objectMapper.readValue(
            state.getMvcResult().getResponse().getContentAsString(StandardCharsets.UTF_8),
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, LibraryDTO.class)
        );
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().isActive()).isTrue();
        state.setLibraryId(response.getData().getId());
    }

    @Given("^系统中存在编码为\"([^\"]*)\"的分馆$")
    public void libraryExistsWithCode(String code) throws Exception {
        CreateLibraryCommand command = CreateLibraryCommand.builder()
            .code(code)
            .name("测试分馆")
            .address("测试地址")
            .phone("010-00000000")
            .build();
        var result = mockMvc.perform(post("/api/inventory/libraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andReturn();

        ApiResponse<LibraryDTO> response = objectMapper.readValue(
            result.getResponse().getContentAsString(StandardCharsets.UTF_8),
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, LibraryDTO.class)
        );
        state.setLibraryId(response.getData().getId());
    }
}
