package com.ai.agents.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Incident Tests")
class IncidentTest {

    @Nested
    @DisplayName("create factory method")
    class CreateFactoryMethodTests {

        @Test
        @DisplayName("should create incident with title, description and severity")
        void shouldCreateIncidentWithTitleDescriptionAndSeverity() {
            Incident incident = Incident.create(
                    "Server Down",
                    "Production server is not responding",
                    Incident.Severity.CRITICAL,
                    List.of("server-1", "server-2")
            );

            assertThat(incident.title()).isEqualTo("Server Down");
            assertThat(incident.description()).isEqualTo("Production server is not responding");
            assertThat(incident.severity()).isEqualTo(Incident.Severity.CRITICAL);
            assertThat(incident.affectedSystems()).containsExactly("server-1", "server-2");
        }

        @Test
        @DisplayName("should set initial status to OPEN")
        void shouldSetInitialStatusToOpen() {
            Incident incident = Incident.create(
                    "Test Incident",
                    "Description",
                    Incident.Severity.WARNING,
                    List.of()
            );

            assertThat(incident.status()).isEqualTo(Incident.IncidentStatus.OPEN);
        }

        @Test
        @DisplayName("should create timeline with created event")
        void shouldCreateTimelineWithCreatedEvent() {
            Incident incident = Incident.create(
                    "Test Incident",
                    "Description",
                    Incident.Severity.INFO,
                    List.of()
            );

            assertThat(incident.timeline()).hasSize(1);
            assertThat(incident.timeline().get(0).eventType()).isEqualTo("created");
        }

        @Test
        @DisplayName("should handle null description")
        void shouldHandleNullDescription() {
            Incident incident = Incident.create(
                    "Test Incident",
                    null,
                    Incident.Severity.INFO,
                    List.of()
            );

            assertThat(incident.description()).isEmpty();
        }

        @Test
        @DisplayName("should handle null affected systems")
        void shouldHandleNullAffectedSystems() {
            Incident incident = Incident.create(
                    "Test Incident",
                    "Description",
                    Incident.Severity.INFO,
                    null
            );

            assertThat(incident.affectedSystems()).isEmpty();
        }

        @Test
        @DisplayName("should generate incident id with prefix")
        void shouldGenerateIncidentIdWithPrefix() {
            Incident incident = Incident.create(
                    "Test",
                    "Desc",
                    Incident.Severity.INFO,
                    List.of()
            );

            assertThat(incident.idValue()).startsWith("inc_");
        }
    }

    @Nested
    @DisplayName("updateStatus method")
    class UpdateStatusMethodTests {

        @Test
        @DisplayName("should update incident status")
        void shouldUpdateIncidentStatus() {
            Incident incident = Incident.create(
                    "Test",
                    "Desc",
                    Incident.Severity.WARNING,
                    List.of()
            );

            Incident updated = incident.updateStatus(
                    Incident.IncidentStatus.INVESTIGATING,
                    "Started investigation"
            );

            assertThat(updated.status()).isEqualTo(Incident.IncidentStatus.INVESTIGATING);
        }

        @Test
        @DisplayName("should add timeline event when updating status")
        void shouldAddTimelineEventWhenUpdatingStatus() {
            Incident incident = Incident.create(
                    "Test",
                    "Desc",
                    Incident.Severity.WARNING,
                    List.of()
            );

            Incident updated = incident.updateStatus(
                    Incident.IncidentStatus.MITIGATED,
                    "Applied fix"
            );

            assertThat(updated.timeline()).hasSize(2);
            assertThat(updated.timeline().get(1).eventType()).isEqualTo("status_change");
        }
    }

    @Nested
    @DisplayName("addTimelineEvent method")
    class AddTimelineEventMethodTests {

        @Test
        @DisplayName("should add custom timeline event")
        void shouldAddCustomTimelineEvent() {
            Incident incident = Incident.create(
                    "Test",
                    "Desc",
                    Incident.Severity.WARNING,
                    List.of()
            );

            Incident updated = incident.addTimelineEvent("note", "Added monitoring");

            assertThat(updated.timeline()).hasSize(2);
            assertThat(updated.timeline().get(1).eventType()).isEqualTo("note");
            assertThat(updated.timeline().get(1).message()).isEqualTo("Added monitoring");
        }
    }

    @Nested
    @DisplayName("addLabel method")
    class AddLabelMethodTests {

        @Test
        @DisplayName("should add label to incident")
        void shouldAddLabelToIncident() {
            Incident incident = Incident.create(
                    "Test",
                    "Desc",
                    Incident.Severity.WARNING,
                    List.of()
            );

            Incident updated = incident.addLabel("team", "oncall");

            assertThat(updated.labels()).containsEntry("team", "oncall");
        }

        @Test
        @DisplayName("should update existing label")
        void shouldUpdateExistingLabel() {
            Incident incident = Incident.create(
                    "Test",
                    "Desc",
                    Incident.Severity.WARNING,
                    List.of()
            ).addLabel("priority", "low");

            Incident updated = incident.addLabel("priority", "high");

            assertThat(updated.labels()).containsEntry("priority", "high");
        }
    }

    @Nested
    @DisplayName("escalate method")
    class EscalateMethodTests {

        @Test
        @DisplayName("should escalate incident severity")
        void shouldEscalateIncidentSeverity() {
            Incident incident = Incident.create(
                    "Test",
                    "Desc",
                    Incident.Severity.WARNING,
                    List.of()
            );

            Incident escalated = incident.escalate(Incident.Severity.CRITICAL);

            assertThat(escalated.severity()).isEqualTo(Incident.Severity.CRITICAL);
        }
    }

    @Nested
    @DisplayName("resolve method")
    class ResolveMethodTests {

        @Test
        @DisplayName("should resolve incident with resolution message")
        void shouldResolveIncidentWithResolutionMessage() {
            Incident incident = Incident.create(
                    "Test",
                    "Desc",
                    Incident.Severity.CRITICAL,
                    List.of()
            );

            Incident resolved = incident.resolve("Restarted the server");

            assertThat(resolved.status()).isEqualTo(Incident.IncidentStatus.RESOLVED);
        }

        @Test
        @DisplayName("should add timeline events when resolving")
        void shouldAddTimelineEventsWhenResolving() {
            Incident incident = Incident.create(
                    "Test",
                    "Desc",
                    Incident.Severity.CRITICAL,
                    List.of()
            );

            Incident resolved = incident.resolve("Fixed the issue");

            assertThat(resolved.timeline()).hasSizeGreaterThan(1);
        }
    }

    @Nested
    @DisplayName("status checking methods")
    class StatusCheckingMethodTests {

        @Test
        @DisplayName("isOpen should return true for OPEN status")
        void isOpenShouldReturnTrueForOpenStatus() {
            Incident incident = Incident.create(
                    "Test",
                    "Desc",
                    Incident.Severity.WARNING,
                    List.of()
            );

            assertThat(incident.isOpen()).isTrue();
        }

        @Test
        @DisplayName("isOpen should return true for INVESTIGATING status")
        void isOpenShouldReturnTrueForInvestigatingStatus() {
            Incident incident = Incident.create(
                    "Test",
                    "Desc",
                    Incident.Severity.WARNING,
                    List.of()
            ).updateStatus(Incident.IncidentStatus.INVESTIGATING, "Investigating");

            assertThat(incident.isOpen()).isTrue();
        }

        @Test
        @DisplayName("isOpen should return false for RESOLVED status")
        void isOpenShouldReturnFalseForResolvedStatus() {
            Incident incident = Incident.create(
                    "Test",
                    "Desc",
                    Incident.Severity.WARNING,
                    List.of()
            ).resolve("Fixed");

            assertThat(incident.isOpen()).isFalse();
        }

        @Test
        @DisplayName("isResolved should return true for RESOLVED status")
        void isResolvedShouldReturnTrueForResolvedStatus() {
            Incident incident = Incident.create(
                    "Test",
                    "Desc",
                    Incident.Severity.WARNING,
                    List.of()
            ).resolve("Fixed");

            assertThat(incident.isResolved()).isTrue();
        }

        @Test
        @DisplayName("isCritical should return true for CRITICAL severity")
        void isCriticalShouldReturnTrueForCriticalSeverity() {
            Incident incident = Incident.create(
                    "Test",
                    "Desc",
                    Incident.Severity.CRITICAL,
                    List.of()
            );

            assertThat(incident.isCritical()).isTrue();
        }
    }

    @Nested
    @DisplayName("TimelineEvent record")
    class TimelineEventTests {

        @Test
        @DisplayName("created factory method should create event")
        void createdFactoryMethodShouldCreateEvent() {
            Instant timestamp = Instant.now();
            Incident.TimelineEvent event = Incident.TimelineEvent.created(timestamp);

            assertThat(event.eventType()).isEqualTo("created");
            assertThat(event.timestamp()).isEqualTo(timestamp);
        }

        @Test
        @DisplayName("statusChange factory method should create event")
        void statusChangeFactoryMethodShouldCreateEvent() {
            Instant timestamp = Instant.now();
            Incident.TimelineEvent event = Incident.TimelineEvent.statusChange(
                    Incident.IncidentStatus.RESOLVED,
                    "Fixed",
                    timestamp
            );

            assertThat(event.eventType()).isEqualTo("status_change");
            assertThat(event.message()).contains("RESOLVED");
            assertThat(event.message()).contains("Fixed");
        }
    }
}
