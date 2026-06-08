package com.ai.gateway.agent;

import com.ai.common.agent.AgentRequest;
import com.ai.common.agent.AgentResponse;
import com.ai.common.agent.AgentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SupervisorAgentTest {

	private SupervisorAgent supervisorAgent;

	@BeforeEach
	void setUp() {
		supervisorAgent = new SupervisorAgent(null, true);
	}

	@Nested
	@DisplayName("Intent Routing Tests")
	class IntentRoutingTests {

		@ParameterizedTest
		@CsvSource({
				"帮我查找文档, RAG",
				"search knowledge base for answers, RAG",
				"检索一下这个问题的答案, RAG",
				"在知识库里找找相关信息, RAG",
				"find relevant documents, RAG"
		})
		@DisplayName("Should route RAG keywords to RAG agent")
		void shouldRouteRagKeywordsToRagAgent(String message, String expectedType) {
			AgentType result = supervisorAgent.routeIntent(message);
			assertThat(result).isEqualTo(AgentType.valueOf(expectedType));
		}

		@ParameterizedTest
		@CsvSource({
				"请把这段文字读出来, TTS",
				"convert to speech, TTS",
				"语音合成这个文本, TTS",
				"speak this aloud, TTS",
				"朗读以下内容, TTS"
		})
		@DisplayName("Should route TTS keywords to TTS agent")
		void shouldRouteTtsKeywordsToTtsAgent(String message, String expectedType) {
			AgentType result = supervisorAgent.routeIntent(message);
			assertThat(result).isEqualTo(AgentType.valueOf(expectedType));
		}

		@ParameterizedTest
		@CsvSource({
				"分析这张图片, VISION",
				"what's in this image, VISION",
				"图片识别一下, VISION",
				"describe the photo, VISION",
				"ocr this document, VISION"
		})
		@DisplayName("Should route Vision keywords to Vision agent")
		void shouldRouteVisionKeywordsToVisionAgent(String message, String expectedType) {
			AgentType result = supervisorAgent.routeIntent(message);
			assertThat(result).isEqualTo(AgentType.valueOf(expectedType));
		}

		@ParameterizedTest
		@CsvSource({
				"生成一张图片, MEDIA",
				"create an image of a cat, MEDIA",
				"画一幅风景画, MEDIA",
				"generate image from text, MEDIA",
				"用 midjourney 生成图片, MEDIA"
		})
		@DisplayName("Should route Media keywords to Media agent")
		void shouldRouteMediaKeywordsToMediaAgent(String message, String expectedType) {
			AgentType result = supervisorAgent.routeIntent(message);
			assertThat(result).isEqualTo(AgentType.valueOf(expectedType));
		}

		@ParameterizedTest
		@CsvSource({
				"翻译成英文, TEXT",
				"translate this to French, TEXT",
				"总结一下这篇文章, TEXT",
				"summarize the document, TEXT",
				"语法检查一下, TEXT"
		})
		@DisplayName("Should route Text keywords to Text agent")
		void shouldRouteTextKeywordsToTextAgent(String message, String expectedType) {
			AgentType result = supervisorAgent.routeIntent(message);
			assertThat(result).isEqualTo(AgentType.valueOf(expectedType));
		}

		@Test
		@DisplayName("Should route generic messages to CHAT agent")
		void shouldRouteGenericMessagesToChatAgent() {
			List<String> genericMessages = List.of(
					"hello",
					"how are you",
					"what's the weather",
					"你好",
					"今天天气怎么样"
			);

			genericMessages.forEach(message -> {
				AgentType result = supervisorAgent.routeIntent(message);
				assertThat(result).isEqualTo(AgentType.CHAT);
			});
		}

		@Test
		@DisplayName("Should return CHAT for null or empty message")
		void shouldReturnChatForNullOrEmptyMessage() {
			assertThat(supervisorAgent.routeIntent(null)).isEqualTo(AgentType.CHAT);
			assertThat(supervisorAgent.routeIntent("")).isEqualTo(AgentType.CHAT);
			assertThat(supervisorAgent.routeIntent("   ")).isEqualTo(AgentType.CHAT);
		}

		@Test
		@DisplayName("Should be case insensitive")
		void shouldBeCaseInsensitive() {
			assertThat(supervisorAgent.routeIntent("SEARCH for documents")).isEqualTo(AgentType.RAG);
			assertThat(supervisorAgent.routeIntent("TTS SYNTHESIS")).isEqualTo(AgentType.TTS);
			assertThat(supervisorAgent.routeIntent("IMAGE GENERATION")).isEqualTo(AgentType.MEDIA);
		}
	}

	@Nested
	@DisplayName("Process Tests")
	class ProcessTests {

		@Test
		@DisplayName("Should return response with routed agent type")
		void shouldReturnResponseWithRoutedAgentType() {
			AgentRequest request = AgentRequest.of("帮我翻译成英文", AgentType.SUPERVISOR);

			AgentResponse response = supervisorAgent.process(request).block();

			assertThat(response).isNotNull();
			assertThat(response.agentType()).isEqualTo(AgentType.TEXT);
			assertThat(response.message()).isEqualTo("帮我翻译成英文");
		}
	}

	@Nested
	@DisplayName("Helper Method Tests")
	class HelperMethodTests {

		@Test
		@DisplayName("Should detect Chinese text")
		void shouldDetectChineseText() {
			assertThat(supervisorAgent.hasChinese("你好世界")).isTrue();
			assertThat(supervisorAgent.hasChinese("hello world")).isFalse();
			assertThat(supervisorAgent.hasChinese("hello 你好 world")).isTrue();
		}
	}
}
