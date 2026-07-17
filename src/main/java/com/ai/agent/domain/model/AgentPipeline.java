package com.ai.agent.domain.model;

import com.ai.agent.domain.vo.AgentType;

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

/**
 * User-authored multi-agent pipeline graph (nodes + directed edges).
 */
public final class AgentPipeline {

    private final List<PipelineNode> nodes;
    private final List<PipelineEdge> edges;

    private AgentPipeline(List<PipelineNode> nodes, List<PipelineEdge> edges) {
        this.nodes = List.copyOf(nodes);
        this.edges = List.copyOf(edges);
    }

    public static AgentPipeline create(List<PipelineNode> nodes, List<PipelineEdge> edges) {
        Objects.requireNonNull(nodes, "nodes");
        Objects.requireNonNull(edges, "edges");
        return new AgentPipeline(nodes, edges);
    }

    public List<PipelineNode> nodes() {
        return nodes;
    }

    public List<PipelineEdge> edges() {
        return edges;
    }

    /**
     * Validates the graph and returns worker agent types in topological order.
     */
    public List<AgentType> executionOrder() {
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("pipeline must contain at least one agent node");
        }

        Map<String, PipelineNode> byId = new LinkedHashMap<>();
        for (PipelineNode node : nodes) {
            if (byId.put(node.id(), node) != null) {
                throw new IllegalArgumentException("duplicate node id: " + node.id());
            }
            if (node.agentType().isSupervisor()) {
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
            if (!byId.containsKey(edge.sourceId()) || !byId.containsKey(edge.targetId())) {
                throw new IllegalArgumentException("edge references unknown node");
            }
            if (edge.sourceId().equals(edge.targetId())) {
                throw new IllegalArgumentException("self-loop edges are not allowed");
            }
            if (outgoing.get(edge.sourceId()).add(edge.targetId())) {
                indegree.merge(edge.targetId(), 1, Integer::sum);
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

        List<AgentType> order = new ArrayList<>();
        Map<String, Integer> remaining = new HashMap<>(indegree);
        while (!ready.isEmpty()) {
            String id = ready.poll();
            order.add(byId.get(id).agentType());
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

    public record PipelineNode(String id, AgentType agentType) {
        public PipelineNode {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(agentType, "agentType");
            if (id.isBlank()) {
                throw new IllegalArgumentException("node id must not be blank");
            }
        }
    }

    public record PipelineEdge(String sourceId, String targetId) {
        public PipelineEdge {
            Objects.requireNonNull(sourceId, "sourceId");
            Objects.requireNonNull(targetId, "targetId");
            if (sourceId.isBlank() || targetId.isBlank()) {
                throw new IllegalArgumentException("edge endpoints must not be blank");
            }
        }
    }
}
