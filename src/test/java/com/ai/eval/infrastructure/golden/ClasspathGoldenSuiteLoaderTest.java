package com.ai.eval.infrastructure.golden;

import com.ai.eval.domain.model.GoldenEvalCase;
import com.ai.eval.domain.vo.GoldenEvalDomain;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ClasspathGoldenSuiteLoader")
class ClasspathGoldenSuiteLoaderTest {

    private final ClasspathGoldenSuiteLoader loader = new ClasspathGoldenSuiteLoader(new ObjectMapper());

    @Test
    @DisplayName("should parse string input and ideal when openai evals shape")
    void shouldParseStringInputAndIdealWhenOpenaiEvalsShape() throws Exception {
        String jsonl = """
                {"id":"c1","input":"What is Explore AI?","ideal":"A demo platform.","metadata":{"domain":"CHAT","tools_enabled":false}}
                """;
        List<GoldenEvalCase> cases = loader.readResource(resource(jsonl));

        assertThat(cases).hasSize(1);
        GoldenEvalCase evalCase = cases.getFirst();
        assertThat(evalCase.id()).isEqualTo("c1");
        assertThat(evalCase.domain()).isEqualTo(GoldenEvalDomain.CHAT);
        assertThat(evalCase.userText()).isEqualTo("What is Explore AI?");
        assertThat(evalCase.ideal()).containsExactly("A demo platform.");
        assertThat(evalCase.toolsEnabled()).isFalse();
    }

    @Test
    @DisplayName("should parse chat format input and ideal array when present")
    void shouldParseChatFormatInputAndIdealArrayWhenPresent() throws Exception {
        String jsonl = """
                {"id":"r1","input":[{"role":"user","content":"Which modules?"}],"ideal":["Chat","RAG"],"metadata":{"domain":"RAG","fixture_keys":["overview"],"contexts":["Chat and RAG"]}}
                """;
        List<GoldenEvalCase> cases = loader.readResource(resource(jsonl));

        assertThat(cases).hasSize(1);
        GoldenEvalCase evalCase = cases.getFirst();
        assertThat(evalCase.domain()).isEqualTo(GoldenEvalDomain.RAG);
        assertThat(evalCase.userText()).isEqualTo("Which modules?");
        assertThat(evalCase.ideal()).containsExactly("Chat", "RAG");
        assertThat(evalCase.fixtureKeys()).containsExactly("overview");
        assertThat(evalCase.contexts()).containsExactly("Chat and RAG");
    }

    @Test
    @DisplayName("should reject line when ideal missing")
    void shouldRejectLineWhenIdealMissing() {
        String jsonl = "{\"id\":\"bad\",\"input\":\"hi\"}\n";
        assertThatThrownBy(() -> loader.readResource(resource(jsonl)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ideal");
    }

    private static ByteArrayResource resource(String jsonl) {
        return new ByteArrayResource(jsonl.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "sample.jsonl";
            }
        };
    }
}
