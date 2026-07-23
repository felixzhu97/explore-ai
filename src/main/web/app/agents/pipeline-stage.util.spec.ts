import {
  appendPipelineStage,
  finalizePipelineStages,
  mergeToolSteps,
} from './pipeline-stage.util';

describe('pipeline-stage.util', () => {
  it('should_markPreviousRunning_when_newHandoffAppended', () => {
    const first = appendPipelineStage([], 'research');
    expect(first).toEqual([
      { name: 'pipeline:research', label: 'research', status: 'running' },
    ]);

    const second = appendPipelineStage(first, 'analyst');
    expect(second).toEqual([
      { name: 'pipeline:research', label: 'research', status: 'success' },
      { name: 'pipeline:analyst', label: 'analyst', status: 'running' },
    ]);
  });

  it('should_finalizeRunningStages_when_streamEnds', () => {
    const stages = appendPipelineStage(
      appendPipelineStage([], 'weather'),
      'analyst',
    );
    expect(finalizePipelineStages(stages, 'success')).toEqual([
      { name: 'pipeline:weather', label: 'weather', status: 'success' },
      { name: 'pipeline:analyst', label: 'analyst', status: 'success' },
    ]);
    expect(finalizePipelineStages(stages, 'error').at(-1)?.status).toBe('error');
  });

  it('should_mergeStagesBeforeDsmlSteps_when_bothPresent', () => {
    const stages = appendPipelineStage([], 'research');
    const dsml = [{ name: 'searchWeb', label: 'searchWeb', status: 'success' as const }];
    expect(mergeToolSteps(stages, dsml)).toEqual([...stages, ...dsml]);
    expect(mergeToolSteps([], dsml)).toEqual(dsml);
    expect(mergeToolSteps(stages, [])).toEqual(stages);
  });
});
