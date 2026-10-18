package com.ai.scim.mapper;

import com.ai.account.domain.model.Account;
import com.ai.account.domain.model.ContactEmail;
import com.ai.scim.service.ScimAccountProfile;
import com.unboundid.scim2.common.types.Email;
import com.unboundid.scim2.common.types.UserResource;
import java.util.List;
import org.springframework.stereotype.Component;

/** Maps accounts to SCIM User resources and back; holds no business rules. */
@Component
public class ScimAccountMapper {

  public static final String RESOURCE_TYPE = "User";

  /** Maps an account to a SCIM User whose {@code externalId} is the IAM user id. */
  public UserResource toResource(Account account, String usersLocation) {
    UserResource resource = new UserResource();
    resource.setId(account.getId().toString());
    resource.setExternalId(account.getSubject());
    resource.setUserName(account.getUserName());
    resource.setDisplayName(account.getDisplayName());
    resource.setActive(account.isActive());
    if (account.getEmail() != null) {
      resource.setEmails(
          List.of(
              new Email()
                  .setValue(account.getEmail().getValue())
                  .setType("work")
                  .setPrimary(true)));
    }
    resource.setMeta(
        ScimMeta.create(
            RESOURCE_TYPE,
            usersLocation + "/" + account.getId(),
            account.getCreatedAt(),
            account.getUpdatedAt(),
            account.getVersion() == null ? 0 : account.getVersion()));
    return resource;
  }

  /** Reads the desired state; an invalid email fails instead of being dropped. */
  public ScimAccountProfile toProfile(UserResource resource) {
    String email = primaryEmail(resource.getEmails());
    return new ScimAccountProfile(
        resource.getExternalId(),
        resource.getUserName(),
        email == null || email.isBlank() ? null : new ContactEmail(email),
        resource.getDisplayName(),
        resource.getActive() == null || resource.getActive());
  }

  private static String primaryEmail(List<Email> emails) {
    if (emails == null || emails.isEmpty()) {
      return null;
    }
    return emails.stream()
        .filter(email -> Boolean.TRUE.equals(email.getPrimary()))
        .findFirst()
        .orElse(emails.get(0))
        .getValue();
  }
}
