import {
  applyPipelineTemplate,
  type PipelineTemplateDefinition,
} from './pipelines.templates';
import { validatePipeline } from './pipelines.model.graph';
import type { AgentInfo } from './pipelines.model';

const catalog: AgentInfo[] = [
  {
    type: 'supervisor',
    name: 'Supervisor',
    description: '',
    healthy: true,
    supervisor: true,
  },
  {
    type: 'research',
    name: 'Research',
    description: 'web',
    healthy: true,
    supervisor: false,
  },
  {
    type: 'weather',
    name: 'Weather',
    description: 'forecast',
    healthy: true,
    supervisor: false,
  },
  {
    type: 'vectordb',
    name: 'Knowledge',
    description: 'rag',
    healthy: true,
    supervisor: false,
  },
  {
    type: 'analyst',
    name: 'Analyst',
    description: 'synth',
    healthy: true,
    supervisor: false,
  },
];

describe('applyPipelineTemplate', () => {
  it('should build valid connected graph when all agents exist', () => {
    const definition: PipelineTemplateDefinition = {
      id: 'competitiveIntel',
      agentTypes: ['research', 'analyst'],
    };

    const result = applyPipelineTemplate(definition, catalog);

    expect(result.skippedAgentTypes).toEqual([]);
    expect(validatePipeline(result.graph)).toEqual({
      ok: true,
      order: ['research', 'analyst'],
    });
    expect(result.graph.nodes.map(n => n.position.x)).toEqual([80, 300]);
  });

  it('should skip missing agent types and reconnect remaining', () => {
    const definition: PipelineTemplateDefinition = {
      id: 'competitiveIntel',
      agentTypes: ['research', 'analyst'],
    };
    const withoutResearch = catalog.filter(agent => agent.type !== 'research');

    const result = applyPipelineTemplate(definition, withoutResearch);

    expect(result.skippedAgentTypes).toEqual(['research']);
    expect(validatePipeline(result.graph)).toEqual({
      ok: true,
      order: ['analyst'],
    });
  });

  it('should return empty graph when no template agents available', () => {
    const definition: PipelineTemplateDefinition = {
      id: 'policyQa',
      agentTypes: ['vectordb', 'analyst'],
    };

    const result = applyPipelineTemplate(definition, [
      catalog.find(a => a.type === 'supervisor')!,
    ]);

    expect(result.skippedAgentTypes).toEqual(['vectordb', 'analyst']);
    expect(result.graph.nodes).toEqual([]);
    expect(validatePipeline(result.graph)).toEqual({
      ok: false,
      reason: 'empty',
    });
  });

  it('should build three node pipeline', () => {
    const definition: PipelineTemplateDefinition = {
      id: 'vendorDiligence',
      agentTypes: ['research', 'vectordb', 'analyst'],
    };

    const result = applyPipelineTemplate(definition, catalog);

    expect(result.skippedAgentTypes).toEqual([]);
    expect(validatePipeline(result.graph)).toEqual({
      ok: true,
      order: ['research', 'vectordb', 'analyst'],
    });
    expect(result.graph.nodes).toHaveLength(3);
    expect(result.graph.connections).toHaveLength(2);
  });
});
