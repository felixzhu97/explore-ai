package com.ai.scim.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.ai.account.domain.model.Account;
import com.ai.account.domain.model.AccountId;
import com.ai.account.domain.model.ExternalIdentity;
import com.ai.account.domain.repository.AccountRepository;
import com.ai.account.service.OwnerErasureService;
import com.ai.common.domain.model.OwnerKey;
import com.ai.scim.infra.ScimFilterTranslator;
import com.ai.scim.mapper.ScimAccountMapper;
import com.ai.testsupport.AbstractDataJpaTest;
import com.unboundid.scim2.common.exceptions.BadRequestException;
import com.unboundid.scim2.common.exceptions.PreconditionFailedException;
import com.unboundid.scim2.common.exceptions.ResourceConflictException;
import com.unboundid.scim2.common.exceptions.ResourceNotFoundException;
import com.unboundid.scim2.common.messages.ListResponse;
import com.unboundid.scim2.common.messages.PatchOperation;
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.types.Email;
import com.unboundid.scim2.common.types.UserResource;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import({ScimAccountService.class, ScimAccountMapper.class, ScimFilterTranslator.class})
@DisplayName("ScimAccountService")
class ScimAccountServiceTest extends AbstractDataJpaTest {

  private static final ScimLocations LOCATIONS =
      new ScimLocations("https://ai.example/scim/v2/Users");

  @Autowired private ScimAccountService service;

  @Autowired private AccountRepository accountRepository;

  @MockitoBean private OwnerErasureService ownerErasureService;

  @Test
  @DisplayName("should create a provisioned account with the IAM identity and a weak ETag")
  void shouldCreateAProvisionedAccountWithTheIamIdentityAndAWeakEtag() throws Exception {
    UserResource created = service.createUser(user("iam-1", "alice"), LOCATIONS);

    assertThat(created.getExternalId()).isEqualTo("iam-1");
    assertThat(created.getUserName()).isEqualTo("alice");
    assertThat(created.getActive()).isTrue();
    assertThat(created.getMeta().getVersion()).isEqualTo("W/\"0\"");
    assertThat(created.getMeta().getLocation().toString())
        .isEqualTo(LOCATIONS.users() + "/" + created.getId());
    assertThat(accountRepository.findByIdentity(ExternalIdentity.createIamIdentity("iam-1")))
        .get()
        .extracting(Account::getUserName)
        .isEqualTo("alice");
  }

  @Test
  @DisplayName("should reject create without externalId")
  void shouldRejectCreateWithoutExternalId() {
    assertThatThrownBy(() -> service.createUser(user(null, "alice"), LOCATIONS))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  @DisplayName("should answer conflict when the externalId or userName is taken")
  void shouldAnswerConflictWhenTheExternalIdOrUserNameIsTaken() throws Exception {
    service.createUser(user("iam-2", "bob"), LOCATIONS);

    assertThatThrownBy(() -> service.createUser(user("iam-2", "bob2"), LOCATIONS))
        .isInstanceOf(ResourceConflictException.class);
    assertThatThrownBy(() -> service.createUser(user("iam-3", "bob"), LOCATIONS))
        .isInstanceOf(ResourceConflictException.class);
  }

  @Test
  @DisplayName("should find accounts by userName and externalId filters only")
  void shouldFindAccountsByUserNameAndExternalIdFiltersOnly() throws Exception {
    service.createUser(user("iam-4", "carol"), LOCATIONS);

    ListResponse<UserResource> byName =
        service.listUsers("userName eq \"carol\"", null, null, LOCATIONS);
    ListResponse<UserResource> byExternalId =
        service.listUsers("externalId eq \"iam-4\"", null, null, LOCATIONS);
    ListResponse<UserResource> none =
        service.listUsers("externalId eq \"iam-404\"", null, null, LOCATIONS);

    assertThat(byName.getTotalResults()).isEqualTo(1);
    assertThat(byExternalId.getResources())
        .extracting(UserResource::getUserName)
        .containsExactly("carol");
    assertThat(none.getTotalResults()).isZero();
    assertThatThrownBy(() -> service.listUsers("displayName eq \"x\"", null, null, LOCATIONS))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  @DisplayName("should list only IAM accounts and page by startIndex and count")
  void shouldListOnlyIamAccountsAndPageByStartIndexAndCount() throws Exception {
    accountRepository.save(
        Account.createAccount(ExternalIdentity.createIdentity("github", "gh-1"), null, "octo"));
    for (int i = 0; i < 3; i++) {
      service.createUser(user("iam-page-" + i, "page" + i), LOCATIONS);
    }

    ListResponse<UserResource> page = service.listUsers(null, 2, 1, LOCATIONS);

    assertThat(page.getTotalResults()).isEqualTo(3);
    assertThat(page.getStartIndex()).isEqualTo(2);
    assertThat(page.getResources()).extracting(UserResource::getUserName).containsExactly("page1");
  }

  @Test
  @DisplayName("should replace the profile, clear left-out values and deactivate")
  void shouldReplaceTheProfileClearLeftOutValuesAndDeactivate() throws Exception {
    UserResource replacement = new UserResource();
    replacement.setUserName("dave2");
    replacement.setActive(false);
    UserResource created = service.createUser(user("iam-5", "dave"), LOCATIONS);
    flushAndClear();

    UserResource replaced = service.replaceUser(created.getId(), replacement, 0L, LOCATIONS);

    assertThat(replaced.getUserName()).isEqualTo("dave2");
    assertThat(replaced.getEmails()).isNull();
    assertThat(replaced.getActive()).isFalse();
    assertThat(replaced.getMeta().getVersion()).isEqualTo("W/\"1\"");
  }

  @Test
  @DisplayName("should fail the precondition when If-Match is stale")
  void shouldFailThePreconditionWhenIfMatchIsStale() throws Exception {
    UserResource created = service.createUser(user("iam-6", "erin"), LOCATIONS);

    assertThatThrownBy(
            () -> service.replaceUser(created.getId(), user(null, "erin2"), 7L, LOCATIONS))
        .isInstanceOf(PreconditionFailedException.class);
  }

  @Test
  @DisplayName("should refuse to change the externalId")
  void shouldRefuseToChangeTheExternalId() throws Exception {
    UserResource created = service.createUser(user("iam-7", "frank"), LOCATIONS);

    assertThatThrownBy(
            () -> service.replaceUser(created.getId(), user("iam-other", "frank"), null, LOCATIONS))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  @DisplayName("should deactivate through a PATCH of active")
  void shouldDeactivateThroughAPatchOfActive() throws Exception {
    UserResource created = service.createUser(user("iam-8", "gina"), LOCATIONS);
    flushAndClear();

    UserResource patched =
        service.patchUser(
            created.getId(),
            new PatchRequest(PatchOperation.replace("active", false)),
            null,
            LOCATIONS);

    assertThat(patched.getActive()).isFalse();
    assertThat(patched.getUserName()).isEqualTo("gina");
    assertThat(patched.getEmails()).extracting(Email::getValue).containsExactly("gina@example.com");
  }

  @Test
  @DisplayName("should erase the owner data before deleting the account")
  void shouldEraseTheOwnerDataBeforeDeletingTheAccount() throws Exception {
    UserResource created = service.createUser(user("iam-9", "hank"), LOCATIONS);
    flushAndClear();

    service.deleteUser(created.getId(), null);
    flushAndClear();

    verify(ownerErasureService).eraseAllForOwner(OwnerKey.createAccountKey(created.getId()));
    assertThat(accountRepository.findById(AccountId.parseId(created.getId()))).isEmpty();
  }

  @Test
  @DisplayName("should hide accounts of other providers")
  void shouldHideAccountsOfOtherProviders() {
    Account github =
        accountRepository.save(
            Account.createAccount(ExternalIdentity.createIdentity("github", "gh-2"), null, null));

    assertThatThrownBy(() -> service.getUser(github.getId().toString(), LOCATIONS))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> service.getUser("not-a-uuid", LOCATIONS))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  private static UserResource user(String externalId, String userName) {
    UserResource resource = new UserResource();
    resource.setExternalId(externalId);
    resource.setUserName(userName);
    resource.setDisplayName(userName);
    resource.setEmails(
        List.of(new Email().setValue(userName + "@example.com").setType("work").setPrimary(true)));
    return resource;
  }
}
