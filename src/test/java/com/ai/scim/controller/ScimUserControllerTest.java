package com.ai.scim.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ai.common.exception.DomainException;
import com.ai.scim.infra.ScimJsonHttpMessageConverter;
import com.ai.scim.infra.ScimSecurityConfig;
import com.ai.scim.mapper.ScimMeta;
import com.ai.scim.service.ScimAccountService;
import com.unboundid.scim2.common.exceptions.PreconditionFailedException;
import com.unboundid.scim2.common.types.Meta;
import com.unboundid.scim2.common.types.UserResource;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.filter.OncePerRequestFilter;

@WebMvcTest(
    controllers = {ScimUserController.class, ScimDiscoveryController.class},
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = OncePerRequestFilter.class))
@Import({ScimSecurityConfig.class, ScimUserControllerTest.SecurityBeans.class})
@ActiveProfiles("test")
@DisplayName("SCIM /Users endpoint")
class ScimUserControllerTest {

  private static final String SCIM = ScimJsonHttpMessageConverter.SCIM_JSON_VALUE;
  private static final SimpleGrantedAuthority SCIM_SCOPE =
      new SimpleGrantedAuthority(ScimSecurityConfig.SCIM_AUTHORITY);

  @Autowired private MockMvc mvc;

  @MockitoBean private ScimAccountService accountService;

  @Test
  @DisplayName("should answer 401 with a SCIM error when no token is sent")
  void shouldAnswer401WithAScimErrorWhenNoTokenIsSent() throws Exception {
    mvc.perform(get("/scim/v2/Users"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().exists("WWW-Authenticate"))
        .andExpect(content().contentTypeCompatibleWith(SCIM))
        .andExpect(jsonPath("$.status").value("401"));
    verifyNoInteractions(accountService);
  }

  @Test
  @DisplayName("should answer 403 when the token lacks write:ai_scim")
  void shouldAnswer403WhenTheTokenLacksWriteAiScim() throws Exception {
    mvc.perform(
            get("/scim/v2/Users")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_write:ai_chat"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value("403"));
    verifyNoInteractions(accountService);
  }

  @Test
  @DisplayName("should advertise only the User resource type")
  void shouldAdvertiseOnlyTheUserResourceType() throws Exception {
    mvc.perform(get("/scim/v2/ResourceTypes").with(jwt().authorities(SCIM_SCOPE)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(SCIM))
        .andExpect(jsonPath("$.totalResults").value(1))
        .andExpect(jsonPath("$.Resources[0].name").value("User"));
    mvc.perform(get("/scim/v2/ServiceProviderConfig").with(jwt().authorities(SCIM_SCOPE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.patch.supported").value(true))
        .andExpect(jsonPath("$.bulk.supported").value(false))
        .andExpect(jsonPath("$.etag.supported").value(true));
  }

  @Test
  @DisplayName("should answer 201 with Location and ETag when creating a user")
  void shouldAnswer201WithLocationAndEtagWhenCreatingAUser() throws Exception {
    when(accountService.createUser(any(), any())).thenReturn(resource("acc-1", 0));

    mvc.perform(
            post("/scim/v2/Users")
                .with(jwt().authorities(SCIM_SCOPE))
                .contentType(SCIM)
                .content(
                    """
                    {"schemas":["urn:ietf:params:scim:schemas:core:2.0:User"],
                     "externalId":"iam-1","userName":"alice"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "https://ai.example/scim/v2/Users/acc-1"))
        .andExpect(header().string("ETag", "W/\"0\""))
        .andExpect(jsonPath("$.userName").value("alice"));
  }

  @Test
  @DisplayName("should answer 412 when If-Match does not match")
  void shouldAnswer412WhenIfMatchDoesNotMatch() throws Exception {
    when(accountService.replaceUser(eq("acc-1"), any(), eq(3L), any()))
        .thenThrow(new PreconditionFailedException("version mismatch"));

    mvc.perform(
            put("/scim/v2/Users/acc-1")
                .with(jwt().authorities(SCIM_SCOPE))
                .header("If-Match", "W/\"3\"")
                .contentType(SCIM)
                .content("{\"userName\":\"alice\"}"))
        .andExpect(status().isPreconditionFailed())
        .andExpect(jsonPath("$.status").value("412"));
  }

  @Test
  @DisplayName("should map a domain rule failure to a SCIM error")
  void shouldMapADomainRuleFailureToAScimError() throws Exception {
    when(accountService.replaceUser(eq("acc-1"), any(), any(), any()))
        .thenThrow(
            DomainException.createInvalidError("USER_NAME_REQUIRED", "userName is required"));

    mvc.perform(
            put("/scim/v2/Users/acc-1")
                .with(jwt().authorities(SCIM_SCOPE))
                .contentType(SCIM)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.scimType").value("invalidValue"))
        .andExpect(jsonPath("$.detail").value("USER_NAME_REQUIRED"));
  }

  @Test
  @DisplayName("should answer 400 invalidSyntax for a malformed body")
  void shouldAnswer400InvalidSyntaxForAMalformedBody() throws Exception {
    mvc.perform(
            post("/scim/v2/Users")
                .with(jwt().authorities(SCIM_SCOPE))
                .contentType(SCIM)
                .content("{not json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.scimType").value("invalidSyntax"));
  }

  private static UserResource resource(String id, long version) {
    UserResource resource = new UserResource();
    resource.setId(id);
    resource.setExternalId("iam-1");
    resource.setUserName("alice");
    resource.setActive(true);
    Meta meta =
        ScimMeta.create(
            "User",
            "https://ai.example/scim/v2/Users/" + id,
            Instant.EPOCH,
            Instant.EPOCH,
            version);
    resource.setMeta(meta);
    return resource;
  }

  @TestConfiguration
  @EnableWebSecurity
  static class SecurityBeans {

    @Bean
    JwtDecoder jwtDecoder() {
      return token -> {
        throw new UnsupportedOperationException("tokens come from the jwt() post-processor");
      };
    }

    @Bean
    JwtAuthenticationConverter iamJwtAuthenticationConverter() {
      return new JwtAuthenticationConverter();
    }
  }
}
