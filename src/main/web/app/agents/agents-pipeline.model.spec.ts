import { validatePipeline, type PipelineGraph } from './agents-pipeline.model';

describe('validatePipeline', () => {
  it('should_accept_single_node_when_no_edges', () => {
    const graph: PipelineGraph = {
      nodes: [{
        id: 'n1',
        agentType: 'k8s',
        name: 'K8s',
        description: 'cluster',
        position: { x: 0, y: 0 },
      }],
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
      nodes: [
        {
          id: 'a',
          agentType: 'k8s',
          name: 'K8s',
          description: '',
          position: { x: 0, y: 0 },
        },
        {
          id: 'b',
          agentType: 'aiops',
          name: 'AIOps',
          description: '',
          position: { x: 100, y: 0 },
        },
      ],
      connections: [],
    };

    expect(validatePipeline(graph)).toEqual({
      ok: false,
      reason: 'needConnections',
    });
  });

  it('should_topo_sort_connected_pipeline', () => {
    const graph: PipelineGraph = {
      nodes: [
        {
          id: 'a',
          agentType: 'k8s',
          name: 'K8s',
          description: '',
          position: { x: 0, y: 0 },
        },
        {
          id: 'b',
          agentType: 'aiops',
          name: 'AIOps',
          description: '',
          position: { x: 200, y: 0 },
        },
      ],
      connections: [{ id: 'c1', sourceNodeId: 'a', targetNodeId: 'b' }],
    };

    expect(validatePipeline(graph)).toEqual({
      ok: true,
      order: ['k8s', 'aiops'],
    });
  });
});
