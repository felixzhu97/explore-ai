package com.ai.image.infrastructure.config;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(ImageProperties.class)
public class ImageModelConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.ai.image.enabled", havingValue = "true", matchIfMissing = true)
    public ImageModel imageModel(ImageProperties properties) {
        ClientOptions clientOptions = ClientOptions.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(normalizeBaseUrl(properties.getBaseUrl()))
                .httpClient(SpringAiOpenAiHttpClient.builder().build())
                .build();

        OpenAIClient openAiClient = new OpenAIClientImpl(clientOptions);

        OpenAiImageOptions defaultOptions = OpenAiImageOptions.builder()
                .model(properties.getModel())
                .build();

        return OpenAiImageModel.builder()
                .openAiClient(openAiClient)
                .options(defaultOptions)
                .build();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "http://localhost:11434/v1";
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
