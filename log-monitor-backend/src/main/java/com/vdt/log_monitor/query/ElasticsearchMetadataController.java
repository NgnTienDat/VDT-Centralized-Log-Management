package com.vdt.log_monitor.query;


import com.vdt.log_monitor.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/es")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ElasticsearchMetadataController {

    private final ElasticsearchMetadataService metadataService;

    @GetMapping("/group-by-fields")
    public ApiResponse<List<String>> getGroupByFields(
            @RequestParam(defaultValue = "sys-logs-*") String index
    ) throws IOException {

        return ApiResponse.<List<String>>builder()
                .data(metadataService.getGroupByFields(index))
                .build();
    }

}