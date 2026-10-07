package com.ai.pipeline.domain.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import lombok.Getter;
import lombok.Value;

/**
 * User-authored multi-agent pipeline graph (nodes + directed edges). Each node carries an editable
 * agent snapshot used at invoke time.
 */
@Getter
public final class AgentPipeline {

  private final List<PipelineNode> nodes;
  private final List<PipelineEdge> edges;

  private AgentPipeline(List<PipelineNode> nodes, List<PipelineEdge> edges) {
    this.nodes = List.copyOf(nodes);
    this.edges = List.copyOf(edges);
  }

  /** Creates a pipeline from nodes and edges. */
  public static AgentPipeline createPipeline(List<PipelineNode> nodes, List<PipelineEdge> edges) {
    Objects.requireNonNull(nodes, "nodes");
    Objects.requireNonNull(edges, "edges");
    return new AgentPipeline(nodes, edges);
  }

  /** Validates the graph and returns worker nodes in topological order. */
  public List<PipelineNode> resolveExecutionOrder() {
    if (nodes.isEmpty()) {
      throw new IllegalArgumentException("pipeline must contain at least one agent node");
    }

    Map<String, PipelineNode> byId = new LinkedHashMap<>();
    for (PipelineNode node : nodes) {
      if (byId.put(node.getId(), node) != null) {
        throw new IllegalArgumentException("duplicate node id: " + node.getId());
      }
      if (node.getAgentType().isSupervisor()) {
        throw new IllegalArgumentException("pipeline nodes must be worker agents, not supervisor");
      }
    }

    Map<String, Set<String>> outgoing = new HashMap<>();
    Map<String, Integer> indegree = new HashMap<>();
    for (String id : byId.keySet()) {
      outgoing.put(id, new HashSet<>());
      indegree.put(id, 0);
    }

    for (PipelineEdge edge : edges) {
      if (!byId.containsKey(edge.getSourceId()) || !byId.containsKey(edge.getTargetId())) {
        throw new IllegalArgumentException("edge references unknown node");
      }
      if (edge.getSourceId().equals(edge.getTargetId())) {
        throw new IllegalArgumentException("self-loop edges are not allowed");
      }
      if (outgoing.get(edge.getSourceId()).add(edge.getTargetId())) {
        indegree.merge(edge.getTargetId(), 1, Integer::sum);
      }
    }

    if (nodes.size() > 1 && edges.isEmpty()) {
      throw new IllegalArgumentException("connect agent nodes before running the pipeline");
    }

    if (nodes.size() > 1 && hasOrphan(byId.keySet(), outgoing)) {
      throw new IllegalArgumentException("all agent nodes must be connected in one pipeline");
    }

    Queue<String> ready = new ArrayDeque<>();
    for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
      if (entry.getValue() == 0) {
        ready.add(entry.getKey());
      }
    }

    List<PipelineNode> order = new ArrayList<>();
    Map<String, Integer> remaining = new HashMap<>(indegree);
    while (!ready.isEmpty()) {
      String id = ready.poll();
      order.add(byId.get(id));
      for (String next : outgoing.get(id)) {
        int nextDegree = remaining.merge(next, -1, Integer::sum);
        if (nextDegree == 0) {
          ready.add(next);
        }
      }
    }

    if (order.size() != nodes.size()) {
      throw new IllegalArgumentException("pipeline contains a cycle");
    }
    return List.copyOf(order);
  }

  private static boolean hasOrphan(Set<String> ids, Map<String, Set<String>> outgoing) {
    Map<String, Set<String>> undirected = new HashMap<>();
    for (String id : ids) {
      undirected.put(id, new HashSet<>());
    }
    for (Map.Entry<String, Set<String>> entry : outgoing.entrySet()) {
      for (String target : entry.getValue()) {
        undirected.get(entry.getKey()).add(target);
        undirected.get(target).add(entry.getKey());
      }
    }
    String start = ids.iterator().next();
    Set<String> visited = new HashSet<>();
    Queue<String> queue = new ArrayDeque<>();
    queue.add(start);
    visited.add(start);
    while (!queue.isEmpty()) {
      String current = queue.poll();
      for (String next : undirected.get(current)) {
        if (visited.add(next)) {
          queue.add(next);
        }
      }
    }
    return visited.size() != ids.size();
  }

  /** Graph node with an editable agent snapshot (double-click edit on canvas). */
  @Value
  public static class PipelineNode {
    String id;
    AgentType agentType;
    String name;
    String description;
    String systemPrompt;
    List<String> tools;

    public PipelineNode(
        String id,
        AgentType agentType,
        String name,
        String description,
        String systemPrompt,
        List<String> tools) {
      Objects.requireNonNull(id, "id");
      Objects.requireNonNull(agentType, "agentType");
      if (id.isBlank()) {
        throw new IllegalArgumentException("node id must not be blank");
      }
      name = name == null || name.isBlank() ? agentType.getValue() : name.trim();
      description = description == null ? "" : description.trim();
      systemPrompt = systemPrompt == null ? "" : systemPrompt.trim();
      tools = tools == null ? List.of() : List.copyOf(tools);
      this.id = id;
      this.agentType = agentType;
      this.name = name;
      this.description = description;
      this.systemPrompt = systemPrompt;
      this.tools = tools;
    }

    /** Creates a node with default settings. */
    public static PipelineNode createNode(String id, AgentType agentType) {
      return new PipelineNode(id, agentType, agentType.getValue(), "", "", List.of());
    }

    /** Tells whether the node carries its own system prompt instead of a catalog agent's. */
    public boolean hasOwnPrompt() {
      return !systemPrompt.isBlank();
    }

    /**
     * Builds the agent definition for this node. A node without its own prompt uses the catalog
     * agent's prompt, and its description and tools when the node leaves them empty.
     */
    public AgentDefinition buildDefinition(AgentDefinition fallback) {
      if (hasOwnPrompt()) {
        return buildDefinition();
      }
      Objects.requireNonNull(fallback, "fallback");
      return AgentDefinition.createDefinition(
          agentType,
          name,
          description.isBlank() ? fallback.getDescription() : description,
          fallback.getSystemPrompt(),
          tools.isEmpty() ? fallback.getTools() : tools,
          AgentDefinition.RUNTIME_SINGLE);
    }

    /** Builds an agent definition from this node, defaulting the prompt when none is set. */
    public AgentDefinition buildDefinition() {
      String prompt =
          systemPrompt.isBlank() ? "You are agent " + agentType.getValue() + "." : systemPrompt;
      return AgentDefinition.createDefinition(
          agentType, name, description, prompt, tools, AgentDefinition.RUNTIME_SINGLE);
    }
  }

  /** Directed link from one pipeline node to the next. */
  @Value
  public static class PipelineEdge {
    String sourceId;
    String targetId;

    public PipelineEdge(String sourceId, String targetId) {
      Objects.requireNonNull(sourceId, "sourceId");
      Objects.requireNonNull(targetId, "targetId");
      if (sourceId.isBlank() || targetId.isBlank()) {
        throw new IllegalArgumentException("edge endpoints must not be blank");
      }
      this.sourceId = sourceId;
      this.targetId = targetId;
    }
  }
}
