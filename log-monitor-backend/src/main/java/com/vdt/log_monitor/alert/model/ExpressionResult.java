package com.vdt.log_monitor.alert.model;

import com.vdt.log_monitor.alert.enums.ResultType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpressionResult {

    private ResultType type;

    private Object value;

    public <T> T getValue(Class<T> clazz) {
        return clazz.cast(value);
    }
}