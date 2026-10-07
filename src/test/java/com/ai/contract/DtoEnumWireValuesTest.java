package com.ai.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.account.controller.dto.AccountMode;
import com.ai.account.controller.dto.AccountPlan;
import com.ai.account.controller.dto.LoginProvider;
import com.ai.automation.domain.model.AutomationActionType;
import com.ai.automation.domain.model.EmailDeliveryStatus;
import com.ai.automation.domain.model.RunStatus;
import com.ai.automation.domain.model.ScheduleKind;
import com.ai.billing.domain.model.Plan;
import com.ai.chat.controller.dto.ProviderStatus;
import com.ai.chat.domain.model.MessageRole;
import com.ai.common.controller.dto.HealthStatus;
import com.ai.image.controller.dto.ImageGenerationStatus;
import com.ai.metrics.controller.dto.MetricsCapability;
import com.ai.metrics.controller.dto.MetricsOutcome;
import com.ai.metrics.controller.dto.MetricsRange;
import com.ai.pipeline.controller.dto.AgentRuntime;
import com.ai.rag.domain.model.DocumentStatus;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("API enum wire values")
class DtoEnumWireValuesTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  static Stream<Arguments> wireValues() {
    return Stream.of(
        Arguments.of(MessageRole.USER, "user"),
        Arguments.of(MessageRole.ASSISTANT, "assistant"),
        Arguments.of(ProviderStatus.AVAILABLE, "available"),
        Arguments.of(ProviderStatus.UNAVAILABLE, "unavailable"),
        Arguments.of(HealthStatus.UP, "UP"),
        Arguments.of(HealthStatus.DOWN, "DOWN"),
        Arguments.of(HealthStatus.DEGRADED, "DEGRADED"),
        Arguments.of(DocumentStatus.READY, "READY"),
        Arguments.of(ScheduleKind.CRON, "CRON"),
        Arguments.of(ScheduleKind.ONCE, "ONCE"),
        Arguments.of(AutomationActionType.RUN_PIPELINE_TEMPLATE, "RUN_PIPELINE_TEMPLATE"),
        Arguments.of(RunStatus.SUCCESS, "SUCCESS"),
        Arguments.of(EmailDeliveryStatus.SENT, "SENT"),
        Arguments.of(MetricsCapability.CHAT, "chat"),
        Arguments.of(MetricsCapability.WORKFLOW, "workflow"),
        Arguments.of(MetricsOutcome.SUCCESS, "success"),
        Arguments.of(MetricsOutcome.ERROR, "error"),
        Arguments.of(MetricsRange.LAST_7_DAYS, "7d"),
        Arguments.of(MetricsRange.LAST_30_DAYS, "30d"),
        Arguments.of(AccountMode.ANONYMOUS, "anonymous"),
        Arguments.of(AccountMode.AUTHENTICATED, "authenticated"),
        Arguments.of(AccountPlan.FREE, "free"),
        Arguments.of(AccountPlan.PRO, "pro"),
        Arguments.of(LoginProvider.GOOGLE, "google"),
        Arguments.of(LoginProvider.GITHUB, "github"),
        Arguments.of(LoginProvider.EXPLORE_IAM, "explore-iam"),
        Arguments.of(ImageGenerationStatus.SUCCESS, "SUCCESS"),
        Arguments.of(AgentRuntime.SINGLE, "single"),
        Arguments.of(AgentRuntime.DEEP, "deep"));
  }

  @ParameterizedTest(name = "{0} serializes as {1}")
  @MethodSource("wireValues")
  @DisplayName("should serialize enum as the existing wire value")
  void shouldSerializeEnumAsTheExistingWireValue(Enum<?> value, String wire) {
    assertThat(JSON.writeValueAsString(value)).isEqualTo("\"" + wire + "\"");
  }

  @Test
  @DisplayName("should read chat role case insensitively when request sends it")
  void shouldReadMessageRoleCaseInsensitivelyWhenRequestSendsIt() {
    assertThat(JSON.readValue("\"User\"", MessageRole.class)).isEqualTo(MessageRole.USER);
  }

  @Test
  @DisplayName("should reject chat role when value is unknown")
  void shouldRejectMessageRoleWhenValueIsUnknown() {
    assertThatThrownBy(() -> JSON.readValue("\"system\"", MessageRole.class))
        .hasRootCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("should map configured plan when casing differs")
  void shouldMapConfiguredPlanWhenCasingDiffers() {
    assertThat(AccountPlan.from(Plan.parse("Pro"))).isEqualTo(AccountPlan.PRO);
    assertThat(AccountPlan.from(Plan.parse("enterprise"))).isEqualTo(AccountPlan.FREE);
  }
}
