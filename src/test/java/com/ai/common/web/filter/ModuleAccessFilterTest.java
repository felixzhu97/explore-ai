package com.ai.common.web.filter;

import com.ai.common.application.featureflag.FeatureFlagService;
import com.ai.common.domain.vo.ModuleFlag;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModuleAccessFilter")
class ModuleAccessFilterTest {

    @Mock
    private FeatureFlagService featureFlagService;

    @Mock
    private FilterChain filterChain;

    private ModuleAccessFilter filter;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new ModuleAccessFilter(featureFlagService);
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("should return 404 when eval module is disabled")
    void should_return404_when_evalModuleDisabled() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/eval/chat");
        when(featureFlagService.isModuleEnabled(ModuleFlag.EVAL)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(404);
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("should continue chain when module is enabled")
    void should_continueChain_when_moduleEnabled() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/mcp/health");
        when(featureFlagService.isModuleEnabled(ModuleFlag.MCP)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should continue chain for unguarded paths")
    void should_continueChain_when_pathNotMapped() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/chat/providers");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(featureFlagService);
    }
}
