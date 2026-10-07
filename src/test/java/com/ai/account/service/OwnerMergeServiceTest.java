package com.ai.account.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ai.account.domain.model.AccountUser;
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
    AccountUser user = AccountUser.create(ExternalIdentity.of("google", "sub"), null, null);
    user.linkBrowser(ClientId.parse(CLIENT_ID), null, null);

    useCase.mergeGuestIntoAccount(user);

    verify(ownerPartitionRepository)
        .reassignOwner(OwnerKey.forClient(CLIENT_ID), OwnerKey.forAccount(user.getId().value()));
  }

  @Test
  @DisplayName("should refuse to merge when the account has no linked browser")
  void shouldRefuseToMergeWhenTheAccountHasNoLinkedBrowser() {
    AccountUser user = AccountUser.create(ExternalIdentity.of("google", "sub"), null, null);

    assertThatThrownBy(() -> useCase.mergeGuestIntoAccount(user))
        .isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(ownerPartitionRepository);
  }
}
