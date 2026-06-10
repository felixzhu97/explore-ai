package com.ai.media.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Infrastructure configuration for Media module.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(MediaProperties.class)
public class MediaConfig {

    private final MediaProperties properties;

    public MediaConfig(MediaProperties properties) {
        this.properties = properties;
    }

    @Bean
    public WebClient stableDiffusionWebClient(WebClient.Builder builder) {
        MediaProperties.StableDiffusion sd = properties.stableDiffusion();
        MediaProperties.HttpConfig httpConfig = sd.http();

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(httpConfig.readTimeout())
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 
                        (int) httpConfig.connectTimeout().toMillis());

        return builder
                .baseUrl(sd.apiUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
