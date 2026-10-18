package com.ai.scim.service;

import com.ai.account.domain.model.Account;
import com.ai.account.domain.model.AccountId;
import com.ai.account.domain.model.ExternalIdentity;
import com.ai.account.domain.repository.AccountRepository;
import com.ai.account.service.OwnerErasureService;
import com.ai.scim.infra.ScimFilterTranslator;
import com.ai.scim.mapper.ScimAccountMapper;
import com.unboundid.scim2.common.GenericScimResource;
import com.unboundid.scim2.common.exceptions.BadRequestException;
import com.unboundid.scim2.common.exceptions.PreconditionFailedException;
import com.unboundid.scim2.common.exceptions.ResourceConflictException;
import com.unboundid.scim2.common.exceptions.ResourceNotFoundException;
import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.filters.Filter;
import com.unboundid.scim2.common.messages.ListResponse;
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.utils.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serves SCIM {@code /Users} for Explore IAM. Only accounts of the {@code explore-iam} provider are
 * visible; Google and GitHub accounts never leave through SCIM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScimAccountService {

  /** Default and maximum page size. */
  public static final int MAX_RESULTS = 200;

  private static final String PROVIDER = ExternalIdentity.EXPLORE_IAM;

  private final AccountRepository accountRepository;
  private final OwnerErasureService ownerErasureService;
  private final ScimAccountMapper mapper;
  private final ScimFilterTranslator filterTranslator;

  /** Lists accounts; filters are limited to {@code userName eq} and {@code externalId eq}. */
  @Transactional(readOnly = true)
  public ListResponse<UserResource> listUsers(
      String filter, Integer startIndex, Integer count, ScimLocations locations)
      throws ScimException {
    int start = startIndex == null || startIndex < 1 ? 1 : startIndex;
    int size = count == null ? MAX_RESULTS : Math.max(0, Math.min(count, MAX_RESULTS));
    Optional<Filter> parsed = filterTranslator.parse(filter);
    if (parsed.isPresent()) {
      List<UserResource> matches = toResources(lookup(parsed.get()), locations);
      int from = Math.min(start - 1, matches.size());
      int to = Math.min(from + size, matches.size());
      return new ListResponse<>(
          matches.size(), List.copyOf(matches.subList(from, to)), start, to - from);
    }
    long total = accountRepository.countByProvider(PROVIDER);
    List<UserResource> items = toResources(window(start - 1, size), locations);
    return new ListResponse<>((int) total, items, start, items.size());
  }

  /** Returns one account. */
  @Transactional(readOnly = true)
  public UserResource getUser(String id, ScimLocations locations) throws ScimException {
    return mapper.toResource(requireAccount(id), locations.users());
  }

  /** Creates a Provisioned Account; the identity is {@code explore-iam} + {@code externalId}. */
  @Transactional
  public UserResource createUser(UserResource body, ScimLocations locations) throws ScimException {
    ScimAccountProfile profile = mapper.toProfile(body);
    if (profile.externalId() == null || profile.externalId().isBlank()) {
      throw BadRequestException.invalidValue("externalId is required");
    }
    ExternalIdentity identity = ExternalIdentity.createIamIdentity(profile.externalId());
    if (accountRepository.findByIdentity(identity).isPresent()) {
      throw ResourceConflictException.uniqueness("externalId already exists");
    }
    requireFreeUserName(profile.userName(), null);
    Account account =
        Account.createProvisionedAccount(
            identity, profile.userName(), profile.email(), profile.displayName());
    applyActive(account, profile.active());
    Account saved = accountRepository.saveAndFlush(account);
    log.info("SCIM created account {}", saved.getId());
    return mapper.toResource(saved, locations.users());
  }

  /** Replaces the account; values left out are cleared. */
  @Transactional
  public UserResource replaceUser(
      String id, UserResource body, Long expectedVersion, ScimLocations locations)
      throws ScimException {
    Account account = requireAccount(id);
    requireVersion(account, expectedVersion);
    Account saved = replace(account, mapper.toProfile(body));
    log.info("SCIM replaced account {}", saved.getId());
    return mapper.toResource(saved, locations.users());
  }

  /** Applies a PATCH request, then stores the result as a replace. */
  @Transactional
  public UserResource patchUser(
      String id, PatchRequest patch, Long expectedVersion, ScimLocations locations)
      throws ScimException {
    Account account = requireAccount(id);
    requireVersion(account, expectedVersion);
    if (patch.getOperations() == null || patch.getOperations().isEmpty()) {
      throw BadRequestException.invalidSyntax("Operations is required");
    }
    GenericScimResource patched =
        patch.apply(mapper.toResource(account, locations.users()).asGenericScimResource());
    UserResource resource;
    try {
      resource = JsonUtils.nodeToValue(patched.getObjectNode(), UserResource.class);
    } catch (RuntimeException ex) {
      throw BadRequestException.invalidValue("patched resource is invalid");
    }
    Account saved = replace(account, mapper.toProfile(resource));
    log.info("SCIM patched account {}", saved.getId());
    return mapper.toResource(saved, locations.users());
  }

  /** Erases all data of the account, then deletes it. */
  @Transactional
  public void deleteUser(String id, Long expectedVersion) throws ScimException {
    Account account = requireAccount(id);
    requireVersion(account, expectedVersion);
    ownerErasureService.eraseAllForOwner(account.createOwnerKey());
    accountRepository.delete(account);
    log.info("SCIM deleted account {}", account.getId());
  }

  private Account replace(Account account, ScimAccountProfile profile) throws ScimException {
    if (profile.externalId() != null && !profile.externalId().equals(account.getSubject())) {
      throw BadRequestException.mutability("externalId cannot change");
    }
    requireFreeUserName(profile.userName(), account.getId());
    account.replaceProfile(profile.userName(), profile.email(), profile.displayName());
    applyActive(account, profile.active());
    return accountRepository.saveAndFlush(account);
  }

  private static void applyActive(Account account, boolean active) {
    if (active) {
      account.reactivateAccount();
    } else {
      account.deactivateAccount();
    }
  }

  private void requireFreeUserName(String userName, AccountId self) throws ScimException {
    if (userName == null || userName.isBlank()) {
      return;
    }
    Optional<Account> holder = accountRepository.findByProviderAndUserName(PROVIDER, userName);
    if (holder.isPresent() && !holder.get().getId().equals(self)) {
      throw ResourceConflictException.uniqueness("userName already exists");
    }
  }

  private Account requireAccount(String id) throws ScimException {
    AccountId accountId;
    try {
      accountId = AccountId.parseId(id);
    } catch (IllegalArgumentException ex) {
      throw new ResourceNotFoundException("account not found");
    }
    return accountRepository
        .findById(accountId)
        .filter(account -> PROVIDER.equals(account.getProvider()))
        .orElseThrow(() -> new ResourceNotFoundException("account not found"));
  }

  private static void requireVersion(Account account, Long expectedVersion) throws ScimException {
    long current = account.getVersion() == null ? 0 : account.getVersion();
    if (expectedVersion != null && expectedVersion != current) {
      throw new PreconditionFailedException("version mismatch");
    }
  }

  private List<Account> lookup(Filter filter) throws ScimException {
    Optional<String> userName = filterTranslator.equalityValue(filter, "userName");
    if (userName.isPresent()) {
      return accountRepository.findByProviderAndUserName(PROVIDER, userName.get()).stream()
          .toList();
    }
    Optional<String> externalId = filterTranslator.equalityValue(filter, "externalId");
    if (externalId.isPresent()) {
      if (externalId.get().isBlank()) {
        return List.of();
      }
      return accountRepository
          .findByIdentity(ExternalIdentity.createIamIdentity(externalId.get()))
          .stream()
          .toList();
    }
    throw BadRequestException.invalidFilter("only userName eq and externalId eq are supported");
  }

  private List<Account> window(int offset, int size) {
    if (size == 0) {
      return List.of();
    }
    int page = offset / size;
    int skip = offset % size;
    List<Account> rows =
        new ArrayList<>(
            accountRepository
                .findByProviderOrderByCreatedAtAsc(PROVIDER, PageRequest.of(page, size))
                .getContent());
    if (skip > 0) {
      rows.addAll(
          accountRepository
              .findByProviderOrderByCreatedAtAsc(PROVIDER, PageRequest.of(page + 1, size))
              .getContent());
    }
    int from = Math.min(skip, rows.size());
    return rows.subList(from, Math.min(from + size, rows.size()));
  }

  private List<UserResource> toResources(List<Account> accounts, ScimLocations locations) {
    return accounts.stream().map(account -> mapper.toResource(account, locations.users())).toList();
  }
}
