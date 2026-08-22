package com.ai.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DomainEntityMappingRulesTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.ai");

  @Test
  @DisplayName("should not declare Id on feature domain models outside kernel packages")
  void shouldNotDeclareIdOnFeatureDomainModelsOutsideKernelPackages() {
    long violations =
        CLASSES.stream()
            .filter(DomainEntityMappingRulesTest::isFeatureDomainModel)
            .flatMap(javaClass -> javaClass.getFields().stream())
            .filter(field -> field.isAnnotatedWith(Id.class))
            .count();
    assertEquals(0, violations);
  }

  @Test
  @DisplayName("should not declare Version on feature domain models outside kernel packages")
  void shouldNotDeclareVersionOnFeatureDomainModelsOutsideKernelPackages() {
    long violations =
        CLASSES.stream()
            .filter(DomainEntityMappingRulesTest::isFeatureDomainModel)
            .flatMap(javaClass -> javaClass.getFields().stream())
            .filter(field -> field.isAnnotatedWith(Version.class))
            .count();
    assertEquals(0, violations);
  }

  private static boolean isFeatureDomainModel(JavaClass javaClass) {
    String packageName = javaClass.getPackageName();
    return packageName.contains(".domain.model")
        && !packageName.startsWith("com.ai.base")
        && !packageName.startsWith("com.ai.common.domain.model");
  }
}
