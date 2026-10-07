package com.ai.chat.controller;

import com.ai.account.controller.OwnerContext;
import com.ai.account.service.OwnerErasureService;
import com.ai.common.controller.ClientIdentityCookieFactory;
import com.ai.common.domain.model.OwnerKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Privacy controls for the current data owner (guest client or signed-in account).
 *
 * @see <a href="https://gdpr.eu/right-to-be-forgotten/">GDPR right to erasure</a>
 * @see <a href="https://gdpr.eu/article-17-right-to-be-forgotten/">Art. 17 GDPR</a>
 */
@RestController
@RequestMapping("/api/privacy")
@RequiredArgsConstructor
public class PrivacyController {

  private final OwnerContext ownerContext;
  private final OwnerErasureService ownerErasureService;
  private final ClientIdentityCookieFactory cookieFactory;

  /**
   * Clears the client identity cookie and issues a new anonymous id (session fixation / privacy
   * reset).
   */
  @PostMapping("/reset-identity")
  public ResponseEntity<Void> resetIdentity(HttpServletResponse response) {
    String nextId = cookieFactory.generateClientId();
    response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString());
    response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.issue(nextId).toString());
    return ResponseEntity.noContent().build();
  }

  /** Deletes all durable data owned by the current owner key. */
  @DeleteMapping("/sessions")
  public ResponseEntity<Void> eraseAllSessions(HttpServletRequest request) {
    OwnerKey owner = ownerContext.require(request);
    ownerErasureService.eraseAllForOwner(owner);
    return ResponseEntity.noContent().build();
  }
}
