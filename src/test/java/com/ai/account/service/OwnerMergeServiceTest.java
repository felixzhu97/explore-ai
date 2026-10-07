package com.ai.account.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ai.account.domain.model.Account;
import com.ai.account.domain.model.ClientId;
import com.ai.account.domain.model.ExternalIdentity;
import com.ai.account.domain.repository.OwnerPartitionRepository;
import com.ai.common.domain.model.OwnerKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerMergeService")
class OwnerMergeServiceTest {

  private static final String CLIENT_ID = "11111111-1111-1111-1111-111111111111";

  @Mock private OwnerPartitionRepository ownerPartitionRepository;

  @InjectMocks private OwnerMergeService useCase;

  @Test
  @DisplayName("should move the linked browser rows to the account when merging")
  void shouldMoveTheLinkedBrowserRowsToTheAccountWhenMerging() {
    Account account =
        Account.createAccount(ExternalIdentity.createIdentity("google", "sub"), null, null);
    account.linkBrowser(ClientId.parseId(CLIENT_ID), null, null);

    useCase.mergeGuestIntoAccount(account);

    verify(ownerPartitionRepository)
        .reassignOwner(
            OwnerKey.createClientKey(CLIENT_ID),
            OwnerKey.createAccountKey(account.getId().toString()));
  }

  @Test
  @DisplayName("should refuse to merge when the account has no linked browser")
  void shouldRefuseToMergeWhenTheAccountHasNoLinkedBrowser() {
    Account account =
        Account.createAccount(ExternalIdentity.createIdentity("google", "sub"), null, null);

    assertThatThrownBy(() -> useCase.mergeGuestIntoAccount(account))
        .isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(ownerPartitionRepository);
  }
}
