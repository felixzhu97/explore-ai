import {
  applyPipelineTemplate,
  findPipelineTemplate,
  PIPELINE_TEMPLATE_CATALOG,
} from './agents-pipeline.templates';
import { validatePipeline } from './agents-pipeline.model';
import type { AgentInfo } from './agents.model';

const catalog: AgentInfo[] = [
  {
    type: 'supervisor',
    name: 'Supervisor',
    description: '',
    healthy: true,
    supervisor: true,
  },
  {
    type: 'k8s',
    name: 'K8s',
    description: 'cluster',
    healthy: true,
    supervisor: false,
  },
  {
    type: 'monitoring',
    name: 'Monitoring',
    description: 'metrics',
    healthy: true,
    supervisor: false,
  },
  {
    type: 'aiops',
    name: 'AIOps',
    description: 'incidents',
    healthy: true,
    supervisor: false,
  },
  {
    type: 'vectordb',
    name: 'VectorDB',
    description: 'rag',
    healthy: true,
    supervisor: false,
  },
];

describe('applyPipelineTemplate', () => {
  it('should_build_valid_connected_graph_when_all_agents_exist', () => {
    const definition = findPipelineTemplate('incidentTriage')!;

    const result = applyPipelineTemplate(definition, catalog);

    expect(result.skippedAgentTypes).toEqual([]);
    expect(validatePipeline(result.graph)).toEqual({
      ok: true,
      order: ['monitoring', 'aiops', 'k8s'],
    });
    expect(result.graph.nodes.map(n => n.position.x)).toEqual([80, 300, 520]);
  });

  it('should_skip_missing_agent_types_and_reconnect_remaining', () => {
    const definition = findPipelineTemplate('incidentTriage')!;
    const withoutAiops = catalog.filter(agent => agent.type !== 'aiops');

    const result = applyPipelineTemplate(definition, withoutAiops);

    expect(result.skippedAgentTypes).toEqual(['aiops']);
    expect(validatePipeline(result.graph)).toEqual({
      ok: true,
      order: ['monitoring', 'k8s'],
    });
  });

  it('should_return_empty_graph_when_no_template_agents_available', () => {
    const definition = findPipelineTemplate('ragOps')!;

    const result = applyPipelineTemplate(definition, [
      catalog.find(a => a.type === 'supervisor')!,
    ]);

    expect(result.skippedAgentTypes).toEqual(['vectordb', 'aiops']);
    expect(result.graph.nodes).toEqual([]);
    expect(validatePipeline(result.graph)).toEqual({
      ok: false,
      reason: 'empty',
    });
  });

  it('should_expose_at_least_three_builtin_templates', () => {
    expect(PIPELINE_TEMPLATE_CATALOG.length).toBeGreaterThanOrEqual(3);
  });
});
