import { validatePipeline, toPipelineInvokeRequest, type PipelineGraph } from './pipelines.model.graph';

function node(
  id: string,
  agentType: string,
  extras: Partial<{
    name: string;
    description: string;
    systemPrompt: string;
    toolKeys: string[];
    x: number;
    y: number;
  }> = {},
) {
  return {
    id,
    agentType,
    name: extras.name ?? agentType,
    description: extras.description ?? '',
    systemPrompt: extras.systemPrompt ?? '',
    toolKeys: extras.toolKeys ?? [],
    position: { x: extras.x ?? 0, y: extras.y ?? 0 },
  };
}

describe('validatePipeline', () => {
  it('should_accept_single_node_when_no_edges', () => {
    const graph: PipelineGraph = {
      nodes: [node('n1', 'k8s', { name: 'K8s', description: 'cluster' })],
      connections: [],
    };

    expect(validatePipeline(graph)).toEqual({ ok: true, order: ['k8s'] });
  });

  it('should_reject_empty_graph', () => {
    expect(validatePipeline({ nodes: [], connections: [] })).toEqual({
      ok: false,
      reason: 'empty',
    });
  });

  it('should_reject_unconnected_nodes', () => {
    const graph: PipelineGraph = {
      nodes: [node('a', 'k8s'), node('b', 'aiops', { x: 100 })],
      connections: [],
    };

    expect(validatePipeline(graph)).toEqual({
      ok: false,
      reason: 'needConnections',
    });
  });

  it('should_topo_sort_connected_pipeline', () => {
    const graph: PipelineGraph = {
      nodes: [node('a', 'k8s'), node('b', 'aiops', { x: 200 })],
      connections: [{ id: 'c1', sourceNodeId: 'a', targetNodeId: 'b' }],
    };

    expect(validatePipeline(graph)).toEqual({
      ok: true,
      order: ['k8s', 'aiops'],
    });
  });
});

describe('toPipelineInvokeRequest', () => {
  it('should_include_node_snapshot_fields', () => {
    const graph: PipelineGraph = {
      nodes: [
        node('a', 'research', {
          name: 'Custom',
          description: 'd',
          systemPrompt: 'prompt',
          toolKeys: ['web_search'],
        }),
      ],
      connections: [],
    };

    expect(toPipelineInvokeRequest('do work', graph)).toEqual({
      message: 'do work',
      nodes: [
        {
          id: 'a',
          agentType: 'research',
          name: 'Custom',
          description: 'd',
          systemPrompt: 'prompt',
          toolKeys: ['web_search'],
        },
      ],
      edges: [],
    });
  });
});
