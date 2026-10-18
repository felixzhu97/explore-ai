package com.ai.scim.controller;

import static com.ai.scim.infra.ScimJsonHttpMessageConverter.SCIM_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.ai.scim.mapper.ScimMeta;
import com.ai.scim.service.ScimAccountService;
import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.messages.ListResponse;
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.types.UserResource;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** SCIM 2.0 {@code /Users} endpoint for Explore IAM (RFC 7644 §3). */
@RestController
@RequiredArgsConstructor
@RequestMapping(
    path = ScimEndpoints.USERS,
    produces = {SCIM_JSON_VALUE, APPLICATION_JSON_VALUE})
public class ScimUserController {

  private final ScimAccountService accountService;

  /** Lists accounts, optionally filtered by {@code userName eq} or {@code externalId eq}. */
  @GetMapping
  public ListResponse<UserResource> list(
      @RequestParam(required = false) String filter,
      @RequestParam(required = false) Integer startIndex,
      @RequestParam(required = false) Integer count)
      throws ScimException {
    return accountService.listUsers(filter, startIndex, count, ScimEndpoints.currentLocations());
  }

  /** Returns one account with its ETag. */
  @GetMapping("/{id}")
  public ResponseEntity<UserResource> get(@PathVariable String id) throws ScimException {
    return withEtag(
        ResponseEntity.ok(), accountService.getUser(id, ScimEndpoints.currentLocations()));
  }

  /** Creates a Provisioned Account; answers 201 with Location and ETag. */
  @PostMapping(consumes = {SCIM_JSON_VALUE, APPLICATION_JSON_VALUE})
  public ResponseEntity<UserResource> create(@RequestBody UserResource body) throws ScimException {
    UserResource created = accountService.createUser(body, ScimEndpoints.currentLocations());
    return withEtag(
        ResponseEntity.created(URI.create(created.getMeta().getLocation().toString())), created);
  }

  /** Replaces an account; {@code If-Match} is checked when sent. */
  @PutMapping(
      path = "/{id}",
      consumes = {SCIM_JSON_VALUE, APPLICATION_JSON_VALUE})
  public ResponseEntity<UserResource> replace(
      @PathVariable String id,
      @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
      @RequestBody UserResource body)
      throws ScimException {
    return withEtag(
        ResponseEntity.ok(),
        accountService.replaceUser(
            id, body, ScimMeta.parseIfMatch(ifMatch), ScimEndpoints.currentLocations()));
  }

  /** Applies a SCIM PatchOp; {@code If-Match} is checked when sent. */
  @PatchMapping(
      path = "/{id}",
      consumes = {SCIM_JSON_VALUE, APPLICATION_JSON_VALUE})
  public ResponseEntity<UserResource> patch(
      @PathVariable String id,
      @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
      @RequestBody PatchRequest patch)
      throws ScimException {
    return withEtag(
        ResponseEntity.ok(),
        accountService.patchUser(
            id, patch, ScimMeta.parseIfMatch(ifMatch), ScimEndpoints.currentLocations()));
  }

  /** Erases the account's data and deletes it; answers 204. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @PathVariable String id,
      @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch)
      throws ScimException {
    accountService.deleteUser(id, ScimMeta.parseIfMatch(ifMatch));
    return ResponseEntity.noContent().build();
  }

  private static ResponseEntity<UserResource> withEtag(
      ResponseEntity.BodyBuilder builder, UserResource resource) {
    return builder.eTag(resource.getMeta().getVersion()).body(resource);
  }
}
