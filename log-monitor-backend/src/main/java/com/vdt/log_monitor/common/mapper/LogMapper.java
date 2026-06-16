package com.vdt.log_monitor.common.mapper;

import com.vdt.log_monitor.common.dto.LogIngestRequest;
import com.vdt.log_monitor.common.dto.LogMessageDto;
import com.vdt.log_monitor.common.entity.LogDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface LogMapper {

    // =========================================================================
    // 1. Map từ LogIngestRequest -> LogMessageDto
    // =========================================================================
    @Mapping(source = "message", target = "logMessage") // Sửa lệch tên field
    @Mapping(source = "environment", target = "environment", qualifiedByName = "normalizeEnv") // Gọi hàm normalize
    LogMessageDto toDto(LogIngestRequest request);

    // =========================================================================
    // 2. Map từ LogDocument -> LogMessageDto
    // Do cấu trúc các field đã khớp hoàn toàn, MapStruct sẽ tự map mà không cần cấu hình gì thêm
    // =========================================================================
    LogMessageDto toDto(LogDocument document);

    // =========================================================================
    // Kỹ thuật Custom Mapping Logic bằng @Named
    // =========================================================================
    @Named("normalizeEnv")
    default String normalizeEnv(String env) {
        return env != null ? env.toLowerCase().trim() : "unknown";
    }
}
