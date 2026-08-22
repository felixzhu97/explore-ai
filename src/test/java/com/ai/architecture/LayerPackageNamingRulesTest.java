package com.ai.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LayerPackageNamingRulesTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.ai");

  @Test
  @DisplayName("should not use legacy web application or infrastructure layer package names")
  void shouldNotUseLegacyLayerPackageNames() {
    long violations =
        CLASSES.stream()
            .filter(LayerPackageNamingRulesTest::isFeatureModuleClass)
            .filter(LayerPackageNamingRulesTest::usesLegacyLayerPackage)
            .count();
    assertEquals(0, violations);
  }

  private static boolean isFeatureModuleClass(JavaClass javaClass) {
    String packageName = javaClass.getPackageName();
    return packageName.startsWith("com.ai.")
        && !packageName.startsWith("com.ai.base")
        && !packageName.startsWith("com.ai.common");
  }

  private static boolean usesLegacyLayerPackage(JavaClass javaClass) {
    String packageName = javaClass.getPackageName();
    return packageName.contains(".web.")
        || packageName.contains(".application.")
        || packageName.endsWith(".web")
        || packageName.endsWith(".application")
        || packageName.contains(".infrastructure.")
        || packageName.endsWith(".infrastructure");
  }
}
