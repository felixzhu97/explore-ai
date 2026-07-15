package com.ai.chat.web.dto;

import com.ai.chat.domain.vo.WebSource;

public record WebSourceDto(String title, String url, String snippet) {

    public static WebSourceDto from(WebSource source) {
        return new WebSourceDto(source.title(), source.url(), source.snippet());
    }
}
