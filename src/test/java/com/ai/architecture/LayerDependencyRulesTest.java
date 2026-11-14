package com.ai.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

class LayerDependencyRulesTest {

  private static final JavaClasses CLASSES = ProductionClasses.ALL;

  @Test
  @DisplayName("should keep domain free of outer layers when a feature is built")
  void shouldKeepDomainFreeOfOuterLayers() {
    noClasses()
        .that()
        .resideInAPackage("com.ai..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "com.ai..controller..", "com.ai..service..", "com.ai..infra..", "com.ai..mapper..")
        .because(
            "domain is the core every other layer depends on and must stay testable alone."
                + " Fix: declare an interface in <feature>/domain/repository and implement it"
                + " in <feature>/infra, or move the logic up into <feature>/service")
        .check(CLASSES);
  }

  @Test
  @DisplayName("should not reach controllers when service or infra code runs")
  void shouldNotReachControllersFromServiceOrInfra() {
    // Existing violations are frozen in src/test/resources/archunit_store; new ones fail.
    FreezingArchRule.freeze(
            noClasses()
                .that()
                .resideInAnyPackage("com.ai..service..", "com.ai..infra..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.ai..controller..")
                .because(
                    "controllers are the outermost layer and their DTOs are an HTTP contract."
                        + " Fix: pass domain types or a service-level record instead of"
                        + " controller DTOs, and map in <feature>/mapper or the controller"))
        .check(CLASSES);
  }

  @Test
  @DisplayName("should not register domain types as Spring beans")
  void shouldNotRegisterDomainTypesAsSpringBeans() {
    noClasses()
        .that()
        .resideInAPackage("com.ai..domain..")
        .should()
        .beAnnotatedWith(Service.class)
        .orShould()
        .beAnnotatedWith(Component.class)
        .because(
            "domain objects are plain models; wiring belongs to the application layer."
                + " Fix: move the bean to <feature>/service, or to <feature>/infra if it"
                + " implements a domain repository")
        .check(CLASSES);
  }

  @Test
  @DisplayName("should keep AI features unaware of metrics when they report invocations")
  void shouldKeepAiFeaturesUnawareOfMetricsWhenTheyReportInvocations() {
    noClasses()
        .that()
        .resideInAnyPackage(
            "com.ai.chat..",
            "com.ai.rag..",
            "com.ai.pipeline..",
            "com.ai.tools..",
            "com.ai.vision..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.ai.metrics..")
        .because(
            "metrics observe AI calls from the outside so features stay unaware of them."
                + " Fix: publish AiInvocationCompletedEvent or AiInvocationFailedEvent"
                + " from com.ai.common.domain.event instead of calling com.ai.metrics")
        .check(CLASSES);
  }
}
