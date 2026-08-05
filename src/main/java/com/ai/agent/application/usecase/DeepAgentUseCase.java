package com.ai.agent.application.usecase;

import com.ai.agent.application.AgentEvaluator;
import com.ai.agent.application.AgentPlanner;
import com.ai.agent.domain.exception.AgentSessionNotFoundException;
import com.ai.agent.domain.model.AgentEvaluation;
import com.ai.agent.domain.model.AgentPlan;
import com.ai.agent.domain.model.AgentSession;
import com.ai.agent.domain.repository.AgentSessionRepository;
import com.ai.agent.domain.vo.SessionId;
import com.ai.agent.infrastructure.config.DeepAgentProperties;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.ChatClientProfile;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.common.infrastructure.llm.ToolEventChannel;
import com.ai.common.infrastructure.skills.AgentSkillsRuntime;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeepAgentUseCase {

    private final AgentSessionRepository sessions;
    private final AgentPlanner planner;
    private final AgentEvaluator evaluator;
    private final ChatClientProvider chatClients;
    private final ChatMemory chatMemory;
    private final AgentSkillsRuntime skillsRuntime;
    private final DeepAgentProperties properties;
    private final ObjectMapper objectMapper;

    public DeepAgentUseCase(
            AgentSessionRepository sessions,
            AgentPlanner planner,
            AgentEvaluator evaluator,
            ChatClientProvider chatClients,
            ChatMemory chatMemory,
            AgentSkillsRuntime skillsRuntime,
            DeepAgentProperties properties,
            ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.planner = planner;
        this.evaluator = evaluator;
        this.chatClients = chatClients;
        this.chatMemory = chatMemory;
        this.skillsRuntime = skillsRuntime;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public AgentSession createSession(String title, String clientId) {
        return sessions.save(AgentSession.create(title, clientId));
    }

    public List<AgentSession> listSessions(String clientId) {
        return sessions.findByClientId(clientId);
    }

    public AgentSession getSession(String sessionId, String clientId) {
        return requireOwned(SessionId.of(sessionId), clientId);
    }

    public void deleteSession(String sessionId, String clientId) {
        AgentSession session = requireOwned(SessionId.of(sessionId), clientId);
        chatMemory.clear(session.id().conversationId());
        sessions.delete(session.id());
    }

    public Flux<ServerSentEvent<String>> invoke(String sessionId, String message, String clientId) {
        if (message == null || message.isBlank()) {
            return Flux.just(errorEvent("message must not be blank"), doneEvent());
        }
        return Flux.defer(() -> {
                    AgentSession session = loadOrCreate(sessionId, clientId);
                    session.touch();
                    sessions.save(session);
                    return iterate(message.trim(), session.id().conversationId());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex -> Flux.just(
                        errorEvent(ex.getMessage() != null ? ex.getMessage() : "Agent failed"),
                        doneEvent()));
    }

    private Flux<ServerSentEvent<String>> iterate(String message, String convId) {
        int max = Math.max(1, properties.getMaxIterations());
        return Flux.defer(() -> runIterations(message, convId, max, "", 1));
    }

    private Flux<ServerSentEvent<String>> runIterations(
            String message, String convId, int max, String feedback, int iteration) {
        if (iteration > max) {
            return Flux.just(
                    errorEvent("Reached max iterations without a passing evaluation"),
                    doneEvent());
        }
        boolean replan = iteration > 1;
        return Mono.fromCallable(() -> planner.plan(message, feedback))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(plan -> {
                    List<ServerSentEvent<String>> preamble = new ArrayList<>();
                    preamble.add(namedEvent(replan ? "replan" : "plan", planPayload(plan, iteration)));
                    if (plan.thoughts() != null && !plan.thoughts().isBlank()) {
                        preamble.add(namedEvent("thought", Map.of(
                                "text", plan.thoughts(),
                                "iteration", iteration)));
                    }
                    return Flux.concat(
                            Flux.fromIterable(preamble),
                            executeWithTools(message, plan, convId)
                                    .collectList()
                                    .flatMapMany(events -> {
                                        String answer = events.stream()
                                                .filter(e -> "message".equals(e.event()))
                                                .map(ServerSentEvent::data)
                                                .reduce("", String::concat);
                                        return Flux.concat(
                                                Flux.fromIterable(events),
                                                Mono.fromCallable(() -> evaluator.evaluate(message, plan, answer))
                                                        .subscribeOn(Schedulers.boundedElastic())
                                                        .flatMapMany(evaluation -> afterEvaluation(
                                                                evaluation, message, convId, max, iteration)));
                                    }));
                });
    }

    private Flux<ServerSentEvent<String>> afterEvaluation(
            AgentEvaluation evaluation,
            String message,
            String convId,
            int max,
            int iteration) {
        ServerSentEvent<String> evalEvent = namedEvent("evaluation", Map.of(
                "verdict", evaluation.verdict().name(),
                "feedback", evaluation.feedback(),
                "iteration", iteration));
        if (evaluation.passed()) {
            return Flux.just(evalEvent, doneEvent());
        }
        if (evaluation.failed()) {
            String err = evaluation.feedback().isBlank() ? "Agent evaluation failed" : evaluation.feedback();
            return Flux.just(evalEvent, errorEvent(err), doneEvent());
        }
        return Flux.concat(
                Flux.just(evalEvent),
                runIterations(message, convId, max, evaluation.feedback(), iteration + 1));
    }

    private Flux<ServerSentEvent<String>> executeWithTools(String message, AgentPlan plan, String convId) {
        return Flux.defer(() -> {
            ToolEventChannel.setCurrentSessionId(convId);
            Sinks.Many<String> toolSink = ToolEventChannel.open(convId);
            String system = skillsRuntime.augmentSystemPrompt(properties.getSystemPrompt());
            ChatClient client = chatClients.create(
                    TextChatOptions.defaults(),
                    ChatClientProfile.MEMORY_TOOLS,
                    convId);
            String prompt = """
                    User request:
                    %s

                    Current plan:
                    %s

                    Complete the plan and answer the user. Use tools when helpful.
                    """.formatted(message, plan.executionPrompt());

            Flux<ServerSentEvent<String>> toolEvents = ToolEventChannel.asFlux(toolSink)
                    .map(json -> ServerSentEvent.<String>builder().event("tool").data(json).build());

            Flux<ServerSentEvent<String>> textEvents = client.prompt()
                    .system(system)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId))
                    .user(prompt)
                    .stream()
                    .content()
                    .filter(t -> t != null && !t.isEmpty())
                    .map(DeepAgentUseCase::messageEvent)
                    .doFinally(sig -> {
                        ToolEventChannel.close(convId);
                        ToolEventChannel.clearCurrentSessionId();
                    });

            return Flux.merge(toolEvents, textEvents);
        });
    }

    private AgentSession loadOrCreate(String sessionId, String clientId) {
        if (sessionId == null || sessionId.isBlank()) {
            return sessions.save(AgentSession.create("New Agent", clientId));
        }
        SessionId id = SessionId.of(sessionId);
        return sessions.findById(id)
                .map(existing -> {
                    if (!existing.ownedBy(clientId)) {
                        throw new AgentSessionNotFoundException(id);
                    }
                    return existing;
                })
                .orElseGet(() -> sessions.save(AgentSession.createWithId(id, "New Agent", clientId)));
    }

    private AgentSession requireOwned(SessionId id, String clientId) {
        AgentSession session = sessions.findById(id)
                .orElseThrow(() -> new AgentSessionNotFoundException(id));
        if (!session.ownedBy(clientId)) {
            throw new AgentSessionNotFoundException(id);
        }
        return session;
    }

    private Map<String, Object> planPayload(AgentPlan plan, int iteration) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("summary", plan.summary());
        map.put("goal", plan.goal());
        map.put("steps", plan.steps());
        map.put("iteration", iteration);
        return map;
    }

    private ServerSentEvent<String> namedEvent(String event, Object payload) {
        try {
            return ServerSentEvent.<String>builder()
                    .event(event)
                    .data(objectMapper.writeValueAsString(payload))
                    .build();
        } catch (JsonProcessingException e) {
            return ServerSentEvent.<String>builder().event(event).data(String.valueOf(payload)).build();
        }
    }

    static ServerSentEvent<String> messageEvent(String data) {
        return ServerSentEvent.<String>builder().event("message").data(data).build();
    }

    static ServerSentEvent<String> doneEvent() {
        return ServerSentEvent.<String>builder().event("done").data("[DONE]").build();
    }

    static ServerSentEvent<String> errorEvent(String message) {
        return ServerSentEvent.<String>builder().event("error").data(message).build();
    }
}
