export interface PipelineNode {
  id: string;
  agentType: string;
  name: string;
  description: string;
  position: { x: number; y: number };
}

export interface PipelineConnection {
  id: string;
  sourceNodeId: string;
  targetNodeId: string;
}

export interface PipelineGraph {
  nodes: PipelineNode[];
  connections: PipelineConnection[];
}

export interface PipelineInvokeRequest {
  message: string;
  nodes: { id: string; agentType: string }[];
  edges: { sourceId: string; targetId: string }[];
}

export type PipelineValidationResult =
  | { ok: true; order: string[] }
  | { ok: false; reason: string };

export function connectorOutId(nodeId: string): string {
  return `${nodeId}-out`;
}

export function connectorInId(nodeId: string): string {
  return `${nodeId}-in`;
}

export function nodeIdFromConnector(connectorId: string): string {
  return connectorId.replace(/-(out|in)$/, '');
}

export function validatePipeline(graph: PipelineGraph): PipelineValidationResult {
  const { nodes, connections } = graph;
  if (nodes.length === 0) {
    return { ok: false, reason: 'empty' };
  }

  const byId = new Map(nodes.map(node => [node.id, node]));
  const outgoing = new Map<string, Set<string>>();
  const indegree = new Map<string, number>();
  for (const node of nodes) {
    outgoing.set(node.id, new Set());
    indegree.set(node.id, 0);
  }

  for (const edge of connections) {
    if (!byId.has(edge.sourceNodeId) || !byId.has(edge.targetNodeId)) {
      return { ok: false, reason: 'unknownEdge' };
    }
    if (edge.sourceNodeId === edge.targetNodeId) {
      return { ok: false, reason: 'selfLoop' };
    }
    const outs = outgoing.get(edge.sourceNodeId)!;
    if (!outs.has(edge.targetNodeId)) {
      outs.add(edge.targetNodeId);
      indegree.set(edge.targetNodeId, (indegree.get(edge.targetNodeId) ?? 0) + 1);
    }
  }

  if (nodes.length > 1 && connections.length === 0) {
    return { ok: false, reason: 'needConnections' };
  }

  if (nodes.length > 1 && hasOrphan(nodes.map(n => n.id), outgoing)) {
    return { ok: false, reason: 'orphan' };
  }

  const ready = [...indegree.entries()]
    .filter(([, degree]) => degree === 0)
    .map(([id]) => id);
  const remaining = new Map(indegree);
  const order: string[] = [];

  while (ready.length > 0) {
    const id = ready.shift()!;
    order.push(byId.get(id)!.agentType);
    for (const next of outgoing.get(id) ?? []) {
      const nextDegree = (remaining.get(next) ?? 0) - 1;
      remaining.set(next, nextDegree);
      if (nextDegree === 0) {
        ready.push(next);
      }
    }
  }

  if (order.length !== nodes.length) {
    return { ok: false, reason: 'cycle' };
  }
  return { ok: true, order };
}

function hasOrphan(ids: string[], outgoing: Map<string, Set<string>>): boolean {
  const undirected = new Map<string, Set<string>>();
  for (const id of ids) {
    undirected.set(id, new Set());
  }
  for (const [source, targets] of outgoing) {
    for (const target of targets) {
      undirected.get(source)!.add(target);
      undirected.get(target)!.add(source);
    }
  }
  const start = ids[0];
  const visited = new Set<string>([start]);
  const queue = [start];
  while (queue.length > 0) {
    const current = queue.shift()!;
    for (const next of undirected.get(current) ?? []) {
      if (!visited.has(next)) {
        visited.add(next);
        queue.push(next);
      }
    }
  }
  return visited.size !== ids.length;
}

export function toPipelineInvokeRequest(
  message: string,
  graph: PipelineGraph,
): PipelineInvokeRequest {
  return {
    message,
    nodes: graph.nodes.map(node => ({ id: node.id, agentType: node.agentType })),
    edges: graph.connections.map(edge => ({
      sourceId: edge.sourceNodeId,
      targetId: edge.targetNodeId,
    })),
  };
}
