package com.ai.testsupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.ai.account.controller.OwnerContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Shared setup for controller slice tests that resolve owner via {@link OwnerContext}. */
public abstract class AbstractOwnerScopedControllerTest {

  @Autowired protected MockMvcTester mvc;

  @MockitoBean protected OwnerContext ownerContext;

  @BeforeEach
  void stubOwnerContext() {
    lenient().when(ownerContext.requireValue(any())).thenReturn(ownerClientId());
  }

  /** Owner key returned by stubbed {@link OwnerContext#requireValue}. */
  protected String ownerClientId() {
    return OwnerKeyFixtures.CLIENT_FULL_KEY;
  }
}
