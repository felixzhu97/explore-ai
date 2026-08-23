package com.ai.metrics.test.fixture;

import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import java.util.ArrayList;
import java.util.List;

/** In-memory {@link AiInvocationEventRepository} for service-layer unit tests. */
public class FakeAiInvocationEventRepository implements AiInvocationEventRepository {

  public final List<AiInvocationEvent> events = new ArrayList<>();
  public DrilldownQuery lastQuery;

  @Override
  public void save(AiInvocationEvent event) {
    events.add(event);
  }

  @Override
  public PageResult findDrilldown(DrilldownQuery query) {
    lastQuery = query;
    return new PageResult(events, events.size());
  }
}
