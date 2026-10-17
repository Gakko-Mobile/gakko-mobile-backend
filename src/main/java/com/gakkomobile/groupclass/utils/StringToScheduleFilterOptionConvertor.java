package com.gakkomobile.groupclass.utils;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToScheduleFilterOptionConvertor implements Converter<String, ScheduleFilterOption> {

    @Override
    public ScheduleFilterOption convert(String source) {
        if (source.isBlank()) {
            return null;
        }
        return ScheduleFilterOption.valueOf(source.toUpperCase());
    }
}
