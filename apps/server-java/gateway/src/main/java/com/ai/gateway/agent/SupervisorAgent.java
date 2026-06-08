package com.ai.gateway.agent;

import com.ai.common.agent.Agent;
import com.ai.common.agent.AgentRequest;
import com.ai.common.agent.AgentResponse;
import com.ai.common.agent.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class SupervisorAgent implements Agent {

	private static final Logger log = LoggerFactory.getLogger(SupervisorAgent.class);

	private static final Set<String> RAG_KEYWORDS = Set.of(
			"文档", "知识库", "检索", "rag", "search", "find", "查找", "查询",
			"document", "knowledge", "database"
	);

	private static final Set<String> TTS_KEYWORDS = Set.of(
			"语音", "朗读", "读出来", "text to speech", "tts", "speak", "朗读",
			"audio", "声音", "speech", "语音合成"
	);

	private static final Set<String> VISION_KEYWORDS = Set.of(
			"图片", "图像", "图片分析", "vision", "image", "analyze", "识别",
			"看图", "图像识别", "照片", "vision", "ocr"
	);

	private static final Set<String> MEDIA_KEYWORDS = Set.of(
			"生成图片", "画", "image generation", "generate image", "create image",
			"midjourney", "stable diffusion", "draw", "生成图像", "绘图"
	);

	private static final Set<String> TEXT_KEYWORDS = Set.of(
			"翻译", "翻译成", "translate", "summarize", "总结", "摘要",
			"grammar", "语法检查", "纠错", "polish", "润色", "rewrite", "改写"
	);

	private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");

	private final AgentRegistry agentRegistry;
	private final boolean intentRoutingEnabled;

	public SupervisorAgent(
			AgentRegistry agentRegistry,
			@Value("${ai.agent.intent-routing.enabled:true}") boolean intentRoutingEnabled
	) {
		this.agentRegistry = agentRegistry;
		this.intentRoutingEnabled = intentRoutingEnabled;
	}

	@Override
	public String name() {
		return "SupervisorAgent";
	}

	@Override
	public AgentType type() {
		return AgentType.SUPERVISOR;
	}

	@Override
	public Mono<AgentResponse> process(AgentRequest request) {
		return Mono.fromCallable(() -> {
			String message = request.message();
			AgentType targetType = routeIntent(message);
			log.info("Routing message to {} (original: {})", targetType, request.agentType());

			return AgentResponse.success(message, targetType)
					.withMetadata(Map.of("routedTo", targetType.name()));
		}).subscribeOn(Schedulers.boundedElastic());
	}

	public AgentType routeIntent(String message) {
		if (message == null || message.isBlank()) {
			return AgentType.CHAT;
		}

		String lowerMessage = message.toLowerCase();

		if (containsAny(lowerMessage, RAG_KEYWORDS)) {
			return AgentType.RAG;
		}
		if (containsAny(lowerMessage, TTS_KEYWORDS)) {
			return AgentType.TTS;
		}
		if (containsAny(lowerMessage, VISION_KEYWORDS)) {
			return AgentType.VISION;
		}
		if (containsAny(lowerMessage, MEDIA_KEYWORDS)) {
			return AgentType.MEDIA;
		}
		if (containsAny(lowerMessage, TEXT_KEYWORDS)) {
			return AgentType.TEXT;
		}

		return AgentType.CHAT;
	}

	private boolean containsAny(String text, Set<String> keywords) {
		return keywords.stream().anyMatch(text::contains);
	}

	public boolean hasChinese(String text) {
		return CHINESE_PATTERN.matcher(text).find();
	}
}
