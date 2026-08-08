package com.ai.account.application.usecase;

import static org.mockito.Mockito.verify;

import com.ai.account.domain.repository.OwnerPartitionRepository;
import com.ai.common.domain.vo.OwnerKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerMergeUseCaseImpl")
class OwnerMergeUseCaseImplTest {

    @Mock
    private OwnerPartitionRepository ownerPartitionRepository;

    @InjectMocks
    private OwnerMergeUseCaseImpl useCase;

    @Test
    void should_reassignOwner_whenMergingClientIntoAccount() {
        useCase.mergeClientIntoAccount("cid-1", "acct-9");

        verify(ownerPartitionRepository)
                .reassignOwner(OwnerKey.forClient("cid-1"), OwnerKey.forAccount("acct-9"));
    }
}
