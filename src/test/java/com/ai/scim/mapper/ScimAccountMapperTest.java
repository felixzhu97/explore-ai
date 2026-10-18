package com.ai.scim.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.account.domain.model.Account;
import com.ai.account.domain.model.ContactEmail;
import com.ai.account.domain.model.ExternalIdentity;
import com.ai.scim.service.ScimAccountProfile;
import com.unboundid.scim2.common.types.Email;
import com.unboundid.scim2.common.types.UserResource;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ScimAccountMapper")
class ScimAccountMapperTest {

  private final ScimAccountMapper mapper = new ScimAccountMapper();

  @Test
  @DisplayName("should expose the IAM user id as externalId")
  void shouldExposeTheIamUserIdAsExternalId() {
    Account account =
        Account.createProvisionedAccount(
            ExternalIdentity.createIamIdentity("iam-1"),
            "alice",
            new ContactEmail("alice@example.com"),
            "Alice");

    UserResource resource = mapper.toResource(account, "https://ai.example/scim/v2/Users");

    assertThat(resource.getId()).isEqualTo(account.getId().toString());
    assertThat(resource.getExternalId()).isEqualTo("iam-1");
    assertThat(resource.getActive()).isTrue();
    assertThat(resource.getEmails())
        .extracting(Email::getValue)
        .containsExactly("alice@example.com");
    assertThat(resource.getMeta().getVersion()).isEqualTo("W/\"0\"");
  }

  @Test
  @DisplayName("should prefer the primary email and default active to true")
  void shouldPreferThePrimaryEmailAndDefaultActiveToTrue() {
    UserResource resource = new UserResource();
    resource.setUserName("bob");
    resource.setEmails(
        List.of(
            new Email().setValue("other@example.com"),
            new Email().setValue("bob@example.com").setPrimary(true)));

    ScimAccountProfile profile = mapper.toProfile(resource);

    assertThat(profile.email()).isEqualTo(new ContactEmail("bob@example.com"));
    assertThat(profile.active()).isTrue();
  }

  @Test
  @DisplayName("should reject an email that is not an address")
  void shouldRejectAnEmailThatIsNotAnAddress() {
    UserResource resource = new UserResource();
    resource.setUserName("bob");
    resource.setEmails(List.of(new Email().setValue("not-an-email")));

    assertThatThrownBy(() -> mapper.toProfile(resource))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
