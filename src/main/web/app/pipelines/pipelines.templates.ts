import type { AgentInfoResponse } from './pipelines.service';
import type { PipelineConnection, PipelineGraph, PipelineNode } from './pipeline-graph';
import { hasText } from '../shared/presence';

export interface PipelineTemplateInput {
  id: string;
  /** Ordered worker agent types forming a linear pipeline. */
  agentTypes: readonly string[];
}

export interface PipelineTemplateApplyResult {
  graph: PipelineGraph;
  skippedAgentTypes: string[];
}

const NODE_GAP_X = 220;
const NODE_ORIGIN = { x: 80, y: 120 };

/**
 * Expands a template into a connected linear graph, skipping agent types
 * not present (or supervisor-only) in the current catalog.
 */
export function applyPipelineTemplate(
  definition: PipelineTemplateInput,
  agents: readonly AgentInfoResponse[],
  idSeed = 1,
): PipelineTemplateApplyResult {
  const byType = new Map(
    agents
      .filter(agent => !agent.supervisor)
      .map(agent => [agent.type, agent]),
  );

  const skippedAgentTypes: string[] = [];
  const resolved: AgentInfoResponse[] = [];
  for (const type of definition.agentTypes) {
    const agent = byType.get(type);
    if (agent === undefined) {
      skippedAgentTypes.push(type);
      continue;
    }
    resolved.push(agent);
  }

  const nodes: PipelineNode[] = resolved.map((agent, index) => ({
    id: `node-${String(idSeed + index)}`,
    agentType: agent.type,
    name: agent.name,
    description: agent.description,
    systemPrompt: agent.systemPrompt,
    toolKeys: [...agent.toolKeys],
    position: {
      x: NODE_ORIGIN.x + index * NODE_GAP_X,
      y: NODE_ORIGIN.y,
    },
  }));

  const connections: PipelineConnection[] = [];
  let sourceNodeId: string | undefined;
  for (const node of nodes) {
    if (hasText(sourceNodeId)) {
      connections.push({
        id: `edge-${String(idSeed + connections.length)}`,
        sourceNodeId,
        targetNodeId: node.id,
      });
    }
    sourceNodeId = node.id;
  }

  return {
    graph: { nodes, connections },
    skippedAgentTypes,
  };
}
