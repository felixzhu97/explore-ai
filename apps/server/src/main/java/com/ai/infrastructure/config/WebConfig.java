package com.ai.infrastructure.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class WebConfig {

    private static final DataSize MAX_UPLOAD = DataSize.ofMegabytes(100);
    private static final DataSize FILE_THRESHOLD = DataSize.ofMegabytes(2);

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement(
            System.getProperty("java.io.tmpdir"),
            MAX_UPLOAD.toBytes(),
            MAX_UPLOAD.toBytes(),
            (int) FILE_THRESHOLD.toBytes()
        );
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            int maxPost = (int) MAX_UPLOAD.toBytes();
            connector.setMaxPostSize(maxPost);
            connector.setMaxSavePostSize(maxPost);
        });
    }
}
