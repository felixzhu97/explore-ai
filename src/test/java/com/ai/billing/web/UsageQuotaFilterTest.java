package com.ai.billing.web;

import com.ai.billing.infrastructure.config.BillingProperties;
import com.ai.common.web.ClientIdentity;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsageQuotaFilter")
class UsageQuotaFilterTest {

    @Mock
    private FilterChain filterChain;

    private BillingProperties properties;
    private UsageQuotaFilter filter;

    @BeforeEach
    void setUp() {
        properties = new BillingProperties();
        properties.setQuotaEnabled(true);
        properties.setPlan("free");
        properties.setFreeDailyRequests(2);
        filter = new UsageQuotaFilter(properties);
    }

    @Test
    void should_allowRequestAndSetQuotaHeaders_whenUnderDailyLimit() throws Exception {
        MockHttpServletRequest request = postChat("client-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader("X-Quota-Limit")).isEqualTo("2");
        assertThat(response.getHeader("X-Quota-Remaining")).isEqualTo("1");
        assertThat(response.getHeader("X-Quota-Plan")).isEqualTo("free");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void should_rejectWithQuotaExceeded_whenDailyLimitSurpassed() throws Exception {
        MockHttpServletResponse first = new MockHttpServletResponse();
        MockHttpServletResponse second = new MockHttpServletResponse();
        MockHttpServletResponse third = new MockHttpServletResponse();

        filter.doFilter(postChat("client-2"), first, filterChain);
        filter.doFilter(postChat("client-2"), second, filterChain);
        filter.doFilter(postChat("client-2"), third, filterChain);

        assertThat(third.getStatus()).isEqualTo(429);
        assertThat(third.getContentAsString()).contains("QUOTA_EXCEEDED");
        verify(filterChain, times(2)).doFilter(any(), any());
    }

    @Test
    void should_notFilter_whenQuotaDisabled() throws Exception {
        properties.setQuotaEnabled(false);
        MockHttpServletRequest request = postChat("client-3");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getHeader("X-Quota-Limit")).isNull();
    }

    private static MockHttpServletRequest postChat(String clientId) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/chat");
        request.setServletPath("/api/chat");
        request.setAttribute(ClientIdentity.REQUEST_ATTRIBUTE, clientId);
        return request;
    }
}
