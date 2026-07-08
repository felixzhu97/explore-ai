package com.ai.mcp.domain.model;

import com.ai.mcp.domain.exception.InvalidMcpSessionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("McpSession")
class McpSessionTest {

    @Test
    @DisplayName("should open active session")
    void should_open_active_session() {
        McpSession session = McpSession.open("server-1", 3);

        assertThat(session.isActive()).isTrue();
        assertThat(session.toolCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("should close session")
    void should_close_session() {
        McpSession session = McpSession.open("server-1", 1);

        session.close();

        assertThat(session.isActive()).isFalse();
    }

    @Test
    @DisplayName("should reject closing closed session twice")
    void should_reject_closing_closed_session_twice() {
        McpSession session = McpSession.open("server-1", 1);
        session.close();

        assertThatThrownBy(session::close).isInstanceOf(InvalidMcpSessionException.class);
    }
}
