import type { ChatBubbleToolStep } from '../shared/components/chat-shell';

/** Mark prior running stages success and append the new agent as running. */
export function appendPipelineStage(
  stages: ChatBubbleToolStep[],
  agentType: string,
): ChatBubbleToolStep[] {
  const completed = stages.map(step => step.status === 'running' ? { ...step, status: 'success' as const } : step,
  );
  return [
    ...completed,
    {
      name: `pipeline:${agentType}`,
      label: agentType,
      status: 'running',
    },
  ];
}

/** Close any still-running stages when the stream finishes. */
export function finalizePipelineStages(
  stages: ChatBubbleToolStep[],
  status: 'success' | 'error',
): ChatBubbleToolStep[] {
  return stages.map(step => step.status === 'running' ? { ...step, status } : step,
  );
}

/** Prefer pipeline stages first, then DSML-derived tool steps. */
export function mergeToolSteps(
  stages: ChatBubbleToolStep[],
  dsmlSteps: ChatBubbleToolStep[],
): ChatBubbleToolStep[] {
  if (stages.length === 0) {
    return dsmlSteps;
  }
  if (dsmlSteps.length === 0) {
    return stages;
  }
  return [...stages, ...dsmlSteps];
}
