package com.ai.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JavaTimeRulesTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.ai");

  @Test
  @DisplayName("should not depend on legacy date types when java time covers them")
  void shouldNotDependOnLegacyDateTypes() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName("java.util.Date")
        .orShould()
        .dependOnClassesThat()
        .haveFullyQualifiedName("java.util.Calendar")
        .orShould()
        .dependOnClassesThat()
        .haveFullyQualifiedName("java.sql.Timestamp")
        .orShould()
        .dependOnClassesThat()
        .haveFullyQualifiedName("java.sql.Date")
        .orShould()
        .dependOnClassesThat()
        .resideInAPackage("org.joda.time..")
        .check(CLASSES);
  }

  @Test
  @DisplayName("should read the current time from an injected clock")
  void shouldReadTheCurrentTimeFromAnInjectedClock() {
    noClasses().should().callMethod(System.class, "currentTimeMillis").check(CLASSES);
  }
}
