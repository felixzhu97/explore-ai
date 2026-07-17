import type { AgentInfo } from './agents.model';
import type { PipelineConnection, PipelineGraph, PipelineNode } from './agents-pipeline.model';

/** Built-in Agent Pipeline Template ids (i18n under agents.pipeline.templates.items). */
export type PipelineTemplateId = 'incidentTriage' | 'clusterHealth' | 'ragOps';

export interface PipelineTemplateDefinition {
  id: PipelineTemplateId;
  /** Ordered worker agent types forming a linear pipeline. */
  agentTypes: readonly string[];
}

export interface PipelineTemplateApplyResult {
  graph: PipelineGraph;
  skippedAgentTypes: string[];
}

export const PIPELINE_TEMPLATE_CATALOG: readonly PipelineTemplateDefinition[] = [
  {
    id: 'incidentTriage',
    agentTypes: ['monitoring', 'aiops', 'k8s'],
  },
  {
    id: 'clusterHealth',
    agentTypes: ['k8s', 'monitoring'],
  },
  {
    id: 'ragOps',
    agentTypes: ['vectordb', 'aiops'],
  },
] as const;

const NODE_GAP_X = 220;
const NODE_ORIGIN = { x: 80, y: 120 };

/**
 * Expands a template into a connected linear graph, skipping agent types
 * not present (or supervisor-only) in the current catalog.
 */
export function applyPipelineTemplate(
  definition: PipelineTemplateDefinition,
  agents: readonly AgentInfo[],
  idSeed = 1,
): PipelineTemplateApplyResult {
  const byType = new Map(
    agents
      .filter(agent => !agent.supervisor)
      .map(agent => [agent.type, agent]),
  );

  const skippedAgentTypes: string[] = [];
  const resolved: AgentInfo[] = [];
  for (const type of definition.agentTypes) {
    const agent = byType.get(type);
    if (!agent) {
      skippedAgentTypes.push(type);
      continue;
    }
    resolved.push(agent);
  }

  const nodes: PipelineNode[] = resolved.map((agent, index) => ({
    id: `node-${idSeed + index}`,
    agentType: agent.type,
    name: agent.name,
    description: agent.description,
    position: {
      x: NODE_ORIGIN.x + index * NODE_GAP_X,
      y: NODE_ORIGIN.y,
    },
  }));

  const connections: PipelineConnection[] = [];
  for (let i = 0; i < nodes.length - 1; i += 1) {
    connections.push({
      id: `edge-${idSeed + i}`,
      sourceNodeId: nodes[i].id,
      targetNodeId: nodes[i + 1].id,
    });
  }

  return {
    graph: { nodes, connections },
    skippedAgentTypes,
  };
}

export function findPipelineTemplate(
  id: PipelineTemplateId,
): PipelineTemplateDefinition | undefined {
  return PIPELINE_TEMPLATE_CATALOG.find(item => item.id === id);
}
