/**
 * Shared owner-partitioned aggregate bases. The {@code ownerPartition} filter restricts queries and
 * loads by id to a single owner_key when enabled on the session.
 */
@FilterDef(
    name = OwnerPartition.FILTER_NAME,
    parameters = @ParamDef(name = OwnerPartition.OWNER_KEY_PARAMETER, type = String.class),
    defaultCondition = "owner_key = :" + OwnerPartition.OWNER_KEY_PARAMETER,
    applyToLoadByKey = true)
package com.ai.common.domain.model;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
