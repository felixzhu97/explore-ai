package com.ai.agents.application.service;

import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.ToolResult;
import com.ai.agents.domain.service.agents.K8sAgentService;
import com.ai.agents.infrastructure.tools.K8sTools;
import com.ai.agents.presentation.dto.AgentResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("K8sUseCase Tests")
class K8sUseCaseTest {

    @Mock
    private K8sAgentService domainService;

    @Mock
    private K8sTools k8sTools;

    private K8sUseCase k8sUseCase;

    @BeforeEach
    void setUp() {
        k8sUseCase = new K8sUseCase(domainService, k8sTools);
    }

    @Nested
    @DisplayName("listPods")
    class ListPodsTests {

        @Test
        @DisplayName("should return pod list successfully")
        void shouldReturnPodListSuccessfully() {
            String namespace = "default";
            String expectedContent = "pod1, pod2, pod3";
            when(k8sTools.listPods(namespace))
                    .thenReturn(Mono.just(ToolResult.success(expectedContent)));

            StepVerifier.create(k8sUseCase.listPods(namespace))
                    .assertNext(response -> {
                        assertThat(response.message()).isEqualTo(expectedContent);
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return success response even when k8sTools fails")
        void shouldReturnSuccessResponseEvenWhenK8sToolsFails() {
            String namespace = "default";
            when(k8sTools.listPods(namespace))
                    .thenReturn(Mono.just(ToolResult.error("Connection refused")));

            StepVerifier.create(k8sUseCase.listPods(namespace))
                    .assertNext(response -> {
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getPod")
    class GetPodTests {

        @Test
        @DisplayName("should return pod details successfully")
        void shouldReturnPodDetailsSuccessfully() {
            String podName = "my-pod";
            String namespace = "production";
            String expectedContent = "Pod YAML content";
            when(k8sTools.getPod(podName, namespace))
                    .thenReturn(Mono.just(ToolResult.success(expectedContent)));

            StepVerifier.create(k8sUseCase.getPod(podName, namespace))
                    .assertNext(response -> {
                        assertThat(response.message()).isEqualTo(expectedContent);
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("describePod")
    class DescribePodTests {

        @Test
        @DisplayName("should return pod description successfully")
        void shouldReturnPodDescriptionSuccessfully() {
            String podName = "nginx-pod";
            String namespace = "default";
            String expectedContent = "Name: nginx-pod\\nStatus: Running";
            when(k8sTools.describePod(podName, namespace))
                    .thenReturn(Mono.just(ToolResult.success(expectedContent)));

            StepVerifier.create(k8sUseCase.describePod(podName, namespace))
                    .assertNext(response -> {
                        assertThat(response.message()).isEqualTo(expectedContent);
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("listServices")
    class ListServicesTests {

        @Test
        @DisplayName("should return service list successfully")
        void shouldReturnServiceListSuccessfully() {
            String namespace = "default";
            String expectedContent = "kubernetes, nginx-service";
            when(k8sTools.listServices(namespace))
                    .thenReturn(Mono.just(ToolResult.success(expectedContent)));

            StepVerifier.create(k8sUseCase.listServices(namespace))
                    .assertNext(response -> {
                        assertThat(response.message()).isEqualTo(expectedContent);
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("listDeployments")
    class ListDeploymentsTests {

        @Test
        @DisplayName("should return deployment list successfully")
        void shouldReturnDeploymentListSuccessfully() {
            String namespace = "production";
            String expectedContent = "api-deployment, web-deployment";
            when(k8sTools.listDeployments(namespace))
                    .thenReturn(Mono.just(ToolResult.success(expectedContent)));

            StepVerifier.create(k8sUseCase.listDeployments(namespace))
                    .assertNext(response -> {
                        assertThat(response.message()).isEqualTo(expectedContent);
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("scaleDeployment")
    class ScaleDeploymentTests {

        @Test
        @DisplayName("should scale deployment successfully with valid replicas")
        void shouldScaleDeploymentSuccessfullyWithValidReplicas() {
            String deploymentName = "api-deployment";
            String namespace = "default";
            int replicas = 3;
            String expectedContent = "deployment.apps/api-deployment scaled";
            when(domainService.validateCommand("scale"))
                    .thenReturn(K8sAgentService.ValidationResult.valid());
            when(k8sTools.scaleDeployment(deploymentName, replicas, namespace))
                    .thenReturn(Mono.just(ToolResult.success(expectedContent)));

            StepVerifier.create(k8sUseCase.scaleDeployment(deploymentName, replicas, namespace))
                    .assertNext(response -> {
                        assertThat(response.message()).isEqualTo(expectedContent);
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return error when replicas is negative")
        void shouldReturnErrorWhenReplicasIsNegative() {
            String deploymentName = "api-deployment";
            String namespace = "default";
            when(domainService.validateCommand("scale"))
                    .thenReturn(K8sAgentService.ValidationResult.valid());

            StepVerifier.create(k8sUseCase.scaleDeployment(deploymentName, -1, namespace))
                    .assertNext(response -> {
                        assertThat(response.error()).isNotNull();
                        assertThat(response.error()).contains("between 0 and 100");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return error when replicas exceeds 100")
        void shouldReturnErrorWhenReplicasExceeds100() {
            String deploymentName = "api-deployment";
            String namespace = "default";
            when(domainService.validateCommand("scale"))
                    .thenReturn(K8sAgentService.ValidationResult.valid());

            StepVerifier.create(k8sUseCase.scaleDeployment(deploymentName, 101, namespace))
                    .assertNext(response -> {
                        assertThat(response.error()).isNotNull();
                        assertThat(response.error()).contains("between 0 and 100");
                    })
                    .verifyComplete();
        }

        @ParameterizedTest
        @CsvSource({
                "0, true",
                "1, true",
                "50, true",
                "100, true"
        })
        @DisplayName("should accept replicas within valid range")
        void shouldAcceptReplicasWithinValidRange(int replicas, boolean expectedValid) {
            String deploymentName = "api-deployment";
            String namespace = "default";

            when(domainService.validateCommand("scale"))
                    .thenReturn(K8sAgentService.ValidationResult.valid());
            when(k8sTools.scaleDeployment(deploymentName, replicas, namespace))
                    .thenReturn(Mono.just(ToolResult.success("scaled")));

            StepVerifier.create(k8sUseCase.scaleDeployment(deploymentName, replicas, namespace))
                    .assertNext(response -> {
                        if (expectedValid) {
                            assertThat(response.error()).isNull();
                        }
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return error when command validation fails")
        void shouldReturnErrorWhenCommandValidationFails() {
            String deploymentName = "api-deployment";
            String namespace = "default";
            int replicas = 3;
            String errorMessage = "Command not allowed";
            when(domainService.validateCommand("scale"))
                    .thenReturn(K8sAgentService.ValidationResult.invalid(errorMessage));

            StepVerifier.create(k8sUseCase.scaleDeployment(deploymentName, replicas, namespace))
                    .assertNext(response -> {
                        assertThat(response.error()).isEqualTo(errorMessage);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getNodeStatus")
    class GetNodeStatusTests {

        @Test
        @DisplayName("should return node status successfully")
        void shouldReturnNodeStatusSuccessfully() {
            String expectedContent = "NAME STATUS\\nnode1 Ready";
            when(k8sTools.getNodeStatus())
                    .thenReturn(Mono.just(ToolResult.success(expectedContent)));

            StepVerifier.create(k8sUseCase.getNodeStatus())
                    .assertNext(response -> {
                        assertThat(response.message()).isEqualTo(expectedContent);
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getEvents")
    class GetEventsTests {

        @Test
        @DisplayName("should return events successfully")
        void shouldReturnEventsSuccessfully() {
            String namespace = "default";
            String expectedContent = "LAST SEEN TYPE REASON";
            when(k8sTools.getEvents(namespace))
                    .thenReturn(Mono.just(ToolResult.success(expectedContent)));

            StepVerifier.create(k8sUseCase.getEvents(namespace))
                    .assertNext(response -> {
                        assertThat(response.message()).isEqualTo(expectedContent);
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("executeKubectl")
    class ExecuteKubectlTests {

        @Test
        @DisplayName("should execute valid kubectl command")
        void shouldExecuteValidKubectlCommand() {
            String command = "get pods";
            when(domainService.validateCommand(command))
                    .thenReturn(K8sAgentService.ValidationResult.valid());

            StepVerifier.create(k8sUseCase.executeKubectl(command))
                    .assertNext(response -> {
                        assertThat(response.message()).contains("validated");
                        assertThat(response.message()).contains(command);
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return error for invalid command")
        void shouldReturnErrorForInvalidCommand() {
            String command = "delete pods";
            String errorMessage = "Subcommand not allowed: delete";
            when(domainService.validateCommand(command))
                    .thenReturn(K8sAgentService.ValidationResult.invalid(errorMessage));

            StepVerifier.create(k8sUseCase.executeKubectl(command))
                    .assertNext(response -> {
                        assertThat(response.error()).isEqualTo(errorMessage);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return error for dangerous patterns")
        void shouldReturnErrorForDangerousPatterns() {
            String command = "get pods && rm -rf";
            String errorMessage = "Dangerous pattern not allowed: &&";
            when(domainService.validateCommand(command))
                    .thenReturn(K8sAgentService.ValidationResult.invalid(errorMessage));

            StepVerifier.create(k8sUseCase.executeKubectl(command))
                    .assertNext(response -> {
                        assertThat(response.error()).isEqualTo(errorMessage);
                    })
                    .verifyComplete();
        }
    }
}
