package com.ai.account.web;

import com.ai.billing.BillingProperties;
import com.ai.common.web.ClientIdentity;
import com.ai.common.web.ClientIdentityRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountController")
class AccountControllerTest {

    @Mock
    private HttpServletRequest request;

    private AccountController controller;

    @BeforeEach
    void setUp() {
        BillingProperties billing = new BillingProperties();
        billing.setPlan("free");
        controller = new AccountController(billing);
    }

    @Test
    void should_returnAnonymousAccount_whenClientIdentityPresent() {
        when(request.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE)).thenReturn("cid-123");

        var response = controller.me(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().mode()).isEqualTo("anonymous");
        assertThat(response.getBody().clientId()).isEqualTo("cid-123");
        assertThat(response.getBody().plan()).isEqualTo("free");
        assertThat(response.getBody().userId()).isNull();
    }

    @Test
    void should_fail_whenClientIdentityMissing() {
        when(request.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE)).thenReturn(null);

        assertThatThrownBy(() -> controller.me(request))
                .isInstanceOf(ClientIdentityRequiredException.class);
    }
}
