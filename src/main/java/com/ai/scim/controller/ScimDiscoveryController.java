package com.ai.scim.controller;

import static com.ai.scim.infra.ScimJsonHttpMessageConverter.SCIM_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.ai.scim.service.ScimAccountService;
import com.unboundid.scim2.common.exceptions.ResourceNotFoundException;
import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.exceptions.ServerErrorException;
import com.unboundid.scim2.common.messages.ListResponse;
import com.unboundid.scim2.common.types.AuthenticationScheme;
import com.unboundid.scim2.common.types.BulkConfig;
import com.unboundid.scim2.common.types.ChangePasswordConfig;
import com.unboundid.scim2.common.types.ETagConfig;
import com.unboundid.scim2.common.types.FilterConfig;
import com.unboundid.scim2.common.types.PatchConfig;
import com.unboundid.scim2.common.types.ResourceTypeResource;
import com.unboundid.scim2.common.types.SchemaResource;
import com.unboundid.scim2.common.types.ServiceProviderConfigResource;
import com.unboundid.scim2.common.types.SortConfig;
import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.utils.SchemaUtils;
import java.beans.IntrospectionException;
import java.net.URI;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** SCIM 2.0 discovery endpoints (RFC 7644 §4); only the User resource type exists. */
@RestController
@RequestMapping(
    path = ScimEndpoints.BASE,
    produces = {SCIM_JSON_VALUE, APPLICATION_JSON_VALUE})
public class ScimDiscoveryController {

  private static final String DOCUMENTATION = "https://datatracker.ietf.org/doc/html/rfc7644";
  private static final String USER_SCHEMA = SchemaUtils.getSchemaUrn(UserResource.class);

  /** Returns supported features: PATCH, filter and ETag; no Bulk, sort or password change. */
  @GetMapping("/ServiceProviderConfig")
  public ServiceProviderConfigResource serviceProviderConfig() {
    return new ServiceProviderConfigResource(
        DOCUMENTATION,
        new PatchConfig(true),
        new BulkConfig(false, 0, 0),
        new FilterConfig(true, ScimAccountService.MAX_RESULTS),
        new ChangePasswordConfig(false),
        new SortConfig(false),
        new ETagConfig(true),
        List.of(AuthenticationScheme.createOAuth2BearerToken(true)));
  }

  /** Lists resource types. */
  @GetMapping("/ResourceTypes")
  public ListResponse<ResourceTypeResource> resourceTypes() {
    return new ListResponse<>(List.of(userType()));
  }

  /** Returns the User resource type. */
  @GetMapping("/ResourceTypes/{name}")
  public ResourceTypeResource resourceType(@PathVariable String name) throws ScimException {
    if ("User".equals(name)) {
      return userType();
    }
    throw new ResourceNotFoundException("unknown resource type");
  }

  /** Lists schemas. */
  @GetMapping("/Schemas")
  public ListResponse<SchemaResource> schemas() throws ScimException {
    return new ListResponse<>(List.of(userSchema()));
  }

  /** Returns the User schema. */
  @GetMapping("/Schemas/{id}")
  public SchemaResource schemaById(@PathVariable String id) throws ScimException {
    if (USER_SCHEMA.equals(id)) {
      return userSchema();
    }
    throw new ResourceNotFoundException("unknown schema");
  }

  private static ResourceTypeResource userType() {
    return new ResourceTypeResource(
        "User", "User", "Explore AI account", URI.create("/Users"), URI.create(USER_SCHEMA), null);
  }

  private static SchemaResource userSchema() throws ScimException {
    try {
      return SchemaUtils.getSchema(UserResource.class);
    } catch (IntrospectionException ex) {
      throw new ServerErrorException("schema metadata unavailable");
    }
  }
}
