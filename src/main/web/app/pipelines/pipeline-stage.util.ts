import type { ToolStep } from '../chat-shell';

/** Mark prior running stages success and append the new agent as running. */
export function appendPipelineStage(
  stages: ToolStep[],
  agentType: string,
): ToolStep[] {
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
  stages: ToolStep[],
  status: 'success' | 'error',
): ToolStep[] {
  return stages.map(step => step.status === 'running' ? { ...step, status } : step,
  );
}

/** Prefer pipeline stages first, then DSML-derived tool steps. */
export function mergeToolSteps(
  stages: ToolStep[],
  dsmlSteps: ToolStep[],
): ToolStep[] {
  if (stages.length === 0) {
    return dsmlSteps;
  }
  if (dsmlSteps.length === 0) {
    return stages;
  }
  return [...stages, ...dsmlSteps];
}
