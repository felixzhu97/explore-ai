package com.ai.scim.infra;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the SCIM JSON converter ahead of the default Jackson converter. */
@Configuration
public class ScimWebConfig implements WebMvcConfigurer {

  @Override
  public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
    builder.addCustomConverter(new ScimJsonHttpMessageConverter());
  }
}
