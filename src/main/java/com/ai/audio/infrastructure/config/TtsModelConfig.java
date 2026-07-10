package com.ai.audio.infrastructure.config;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(TtsProperties.class)
public class TtsModelConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.ai.tts.enabled", havingValue = "true", matchIfMissing = true)
    public TextToSpeechModel textToSpeechModel(TtsProperties properties) {
        ClientOptions clientOptions = ClientOptions.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(normalizeBaseUrl(properties.getBaseUrl()))
                .httpClient(SpringAiOpenAiHttpClient.builder().build())
                .build();

        OpenAIClient openAiClient = new OpenAIClientImpl(clientOptions);

        OpenAiAudioSpeechOptions defaultOptions = OpenAiAudioSpeechOptions.builder()
                .model(properties.getModel())
                .voice(properties.getVoice())
                .build();

        return OpenAiAudioSpeechModel.builder()
                .openAiClient(openAiClient)
                .options(defaultOptions)
                .build();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "https://api.openai.com/v1";
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
