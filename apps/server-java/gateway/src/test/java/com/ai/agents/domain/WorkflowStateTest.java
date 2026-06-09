package com.ai.agents.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkflowState Tests")
class WorkflowStateTest {

    @Nested
    @DisplayName("start factory method")
    class StartFactoryMethodTests {

        @Test
        @DisplayName("should create workflow with RUNNING status")
        void shouldCreateWorkflowWithRunningStatus() {
            WorkflowState state = WorkflowState.start("test-workflow");

            assertThat(state.status()).isEqualTo(WorkflowState.WorkflowStatus.RUNNING);
            assertThat(state.workflowType()).isEqualTo("test-workflow");
        }

        @Test
        @DisplayName("should initialize with start node as current")
        void shouldInitializeWithStartNodeAsCurrent() {
            WorkflowState state = WorkflowState.start("test");

            assertThat(state.currentNodes()).contains("start");
        }

        @Test
        @DisplayName("should initialize with empty completed nodes")
        void shouldInitializeWithEmptyCompletedNodes() {
            WorkflowState state = WorkflowState.start("test");

            assertThat(state.completedNodes()).isEmpty();
        }

        @Test
        @DisplayName("should start with initial state")
        void shouldStartWithInitialState() {
            Map<String, Object> initialState = Map.of("key", "value");
            WorkflowState state = WorkflowState.start("test", initialState);

            assertThat(state.getStateValue("key")).isEqualTo("value");
        }
    }

    @Nested
    @DisplayName("updateState method")
    class UpdateStateMethodTests {

        @Test
        @DisplayName("should update state with key-value pair")
        void shouldUpdateStateWithKeyValuePair() {
            WorkflowState state = WorkflowState.start("test");

            WorkflowState updated = state.updateState("result", "success");

            assertThat(updated.getStateValue("result")).isEqualTo("success");
        }

        @Test
        @DisplayName("should update state with map")
        void shouldUpdateStateWithMap() {
            WorkflowState state = WorkflowState.start("test");
            Map<String, Object> updates = Map.of("a", 1, "b", 2);

            WorkflowState updated = state.updateState(updates);

            assertThat(updated.getStateValue("a")).isEqualTo(1);
            assertThat(updated.getStateValue("b")).isEqualTo(2);
        }

        @Test
        @DisplayName("should not modify original state")
        void shouldNotModifyOriginalState() {
            WorkflowState state = WorkflowState.start("test");

            state.updateState("key", "value");

            assertThat(state.getStateValue("key")).isNull();
        }
    }

    @Nested
    @DisplayName("moveToNode method")
    class MoveToNodeMethodTests {

        @Test
        @DisplayName("should move to specified node")
        void shouldMoveToSpecifiedNode() {
            WorkflowState state = WorkflowState.start("test");

            WorkflowState moved = state.moveToNode("process");

            assertThat(moved.currentNodes()).contains("process");
        }
    }

    @Nested
    @DisplayName("completeNode method")
    class CompleteNodeMethodTests {

        @Test
        @DisplayName("should mark node as completed")
        void shouldMarkNodeAsCompleted() {
            WorkflowState state = WorkflowState.start("test");

            WorkflowState completed = state.completeNode("start");

            assertThat(completed.isNodeCompleted("start")).isTrue();
        }

        @Test
        @DisplayName("should remove node from current nodes")
        void shouldRemoveNodeFromCurrentNodes() {
            WorkflowState state = WorkflowState.start("test");

            WorkflowState completed = state.completeNode("start");

            assertThat(completed.currentNodes()).doesNotContain("start");
        }
    }

    @Nested
    @DisplayName("complete method")
    class CompleteMethodTests {

        @Test
        @DisplayName("should set status to COMPLETED")
        void shouldSetStatusToCompleted() {
            WorkflowState state = WorkflowState.start("test");

            WorkflowState completed = state.complete();

            assertThat(completed.status()).isEqualTo(WorkflowState.WorkflowStatus.COMPLETED);
            assertThat(completed.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("should move all current nodes to completed")
        void shouldMoveAllCurrentNodesToCompleted() {
            WorkflowState state = WorkflowState.start("test").moveToNode("finish");

            WorkflowState completed = state.complete();

            assertThat(completed.isNodeCompleted("finish")).isTrue();
        }
    }

    @Nested
    @DisplayName("fail method")
    class FailMethodTests {

        @Test
        @DisplayName("should set status to FAILED")
        void shouldSetStatusToFailed() {
            WorkflowState state = WorkflowState.start("test");

            WorkflowState failed = state.fail();

            assertThat(failed.status()).isEqualTo(WorkflowState.WorkflowStatus.FAILED);
            assertThat(failed.isFailed()).isTrue();
        }
    }

    @Nested
    @DisplayName("status checking methods")
    class StatusCheckingMethodTests {

        @Test
        @DisplayName("isRunning should return true for RUNNING status")
        void isRunningShouldReturnTrueForRunningStatus() {
            WorkflowState state = WorkflowState.start("test");

            assertThat(state.isRunning()).isTrue();
        }

        @Test
        @DisplayName("hasPendingNodes should return true when pending nodes exist")
        void hasPendingNodesShouldReturnTrueWhenPendingNodesExist() {
            WorkflowState state = WorkflowState.start("test").addPendingNode("node1");

            assertThat(state.hasPendingNodes()).isTrue();
        }

        @Test
        @DisplayName("hasPendingNodes should return false when no pending nodes")
        void hasPendingNodesShouldReturnFalseWhenNoPendingNodes() {
            WorkflowState state = WorkflowState.start("test");

            assertThat(state.hasPendingNodes()).isFalse();
        }
    }
}
