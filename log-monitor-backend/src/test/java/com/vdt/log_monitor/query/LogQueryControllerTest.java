package com.vdt.log_monitor.query;

import com.vdt.log_monitor.common.dto.CursorPage;
import com.vdt.log_monitor.common.dto.LogMessageDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogQueryController.class)
class LogQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

//    @Autowired
//    private ObjectMapper objectMapper;

    @MockitoBean
    private LogQueryService logQueryService;

    @Test
    @DisplayName("Search logs successfully")
    void searchLogs_ShouldReturnCursorPage() throws Exception {

        CursorPage<LogMessageDto> page = CursorPage.<LogMessageDto>builder()
                .data(List.of())
                .hasMore(false)
                .nextCursor(null)
                .nextCursorId(null)
                .build();

        when(logQueryService.search(any()))
                .thenReturn(page);

        mockMvc.perform(
                        get("/api/v1/logs")
                                .param("environment", "dev")
                                .param("logLevel", "ERROR")
                                .param("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.hasMore").value(false))
                .andExpect(jsonPath("$.data.data").isArray());
    }

    @Test
    @DisplayName("Get log by Elasticsearch id")
    void getLogById_ShouldUseFindById() throws Exception {

        LogMessageDto dto = new LogMessageDto();

        when(logQueryService.findById("abc123"))
                .thenReturn(dto);

        mockMvc.perform(
                        get("/api/v1/logs/abc123")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"));
    }

    @Test
    @DisplayName("Get log by doc_id")
    void getLogByDocId_ShouldUseFindByDocId() throws Exception {

        LogMessageDto dto = new LogMessageDto();

        when(logQueryService.findByDocId("doc123"))
                .thenReturn(dto);

        mockMvc.perform(
                        get("/api/v1/logs/doc123")
                                .param("by", "doc_id")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"));
    }

    @Test
    @DisplayName("Get unique services")
    void getServices_ShouldReturnServices() throws Exception {

        when(logQueryService.getUniqueServicesOrApps("service"))
                .thenReturn(List.of(
                        "logs-service",
                        "user-service",
                        "order-service"
                ));

        mockMvc.perform(
                        get("/api/v1/logs/services")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0]").value("logs-service"));
    }
}