package com.ai.testsupport;

import java.time.Duration;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Helpers for SSE / streaming controller tests with {@link MockMvcTester}. */
public final class MvcStreamTestSupport {

  public static final Duration STREAM_TIMEOUT = Duration.ofSeconds(5);

  private MvcStreamTestSupport() {}

  /** Waits for an async stream response before assertions. */
  public static MvcTestResult exchangeStream(MockMvcTester.MockMvcRequestBuilder request) {
    return request.exchange(STREAM_TIMEOUT);
  }
}
