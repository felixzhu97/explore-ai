package com.ai.chat.web;

import com.ai.chat.application.ChatUseCase;
import com.ai.common.web.ClientIdentity;
import com.ai.common.web.ClientIdentityCookieFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Privacy controls for anonymous browser-scoped chat data.
 *
 * @see <a href="https://gdpr.eu/right-to-be-forgotten/">GDPR right to erasure</a>
 * @see <a href="https://gdpr.eu/article-17-right-to-be-forgotten/">Art. 17 GDPR</a>
 */
@RestController
@RequestMapping("/api/privacy")
public class PrivacyController {

    private final ChatUseCase chatUseCase;
    private final ClientIdentityCookieFactory cookieFactory;

    public PrivacyController(ChatUseCase chatUseCase, ClientIdentityCookieFactory cookieFactory) {
        this.chatUseCase = chatUseCase;
        this.cookieFactory = cookieFactory;
    }

    /**
     * Deletes all chat sessions owned by the current browser identity.
     */
    @DeleteMapping("/sessions")
    public ResponseEntity<Void> eraseAllSessions(HttpServletRequest request) {
        chatUseCase.deleteAllSessionsForClient(ClientIdentity.require(request));
        return ResponseEntity.noContent().build();
    }

    /**
     * Clears the client identity cookie and issues a new anonymous id (session fixation / privacy reset).
     */
    @PostMapping("/reset-identity")
    public ResponseEntity<Void> resetIdentity(HttpServletResponse response) {
        String nextId = cookieFactory.newClientId();
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.issue(nextId).toString());
        return ResponseEntity.noContent().build();
    }
}
