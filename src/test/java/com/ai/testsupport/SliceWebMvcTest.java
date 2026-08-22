package com.ai.testsupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Slice test for a single controller without loading application {@link OncePerRequestFilter} beans
 * (billing quota, client identity, CSRF, etc.).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest(
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = OncePerRequestFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "launchdarkly.bootstrap.module-vision=true",
      "launchdarkly.bootstrap.module-mcp=true",
      "launchdarkly.bootstrap.module-eval=true",
      "launchdarkly.bootstrap.module-pipelines=true",
      "launchdarkly.bootstrap.module-automations=true",
      "launchdarkly.bootstrap.module-skills=true",
      "launchdarkly.bootstrap.module-audio-asr=true"
    })
public @interface SliceWebMvcTest {

  /** Controllers under test; forwarded to {@link WebMvcTest#controllers()}. */
  @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
  Class<?>[] controllers() default {};
}
