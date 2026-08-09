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
@DisplayName("OwnerEraseUseCaseImpl")
class OwnerEraseUseCaseImplTest {

    @Mock
    private OwnerPartitionRepository ownerPartitionRepository;

    @InjectMocks
    private OwnerEraseUseCaseImpl useCase;

    @Test
    void shouldDeleteAllForOwnerWhenEraseRequested() {
        OwnerKey owner = OwnerKey.forAccount("acct-1");

        useCase.eraseAllForOwner(owner);

        verify(ownerPartitionRepository).deleteAllForOwner(owner);
    }
}
