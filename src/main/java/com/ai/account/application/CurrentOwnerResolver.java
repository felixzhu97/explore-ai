package com.ai.account.application;

import com.ai.common.domain.vo.OwnerKey;
import org.springframework.security.core.Authentication;

/**
 * Resolves the data partition for the current visitor (guest client or signed-in account).
 */
public interface CurrentOwnerResolver {

    OwnerKey resolve(String clientId, Authentication authentication);
}
