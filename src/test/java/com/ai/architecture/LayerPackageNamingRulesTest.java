package com.ai.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LayerPackageNamingRulesTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.ai");

  private static final Set<String> FORBIDDEN_PACKAGE_SEGMENTS =
      Set.of("usecase", "adapter", "port");

  private static final List<String> FORBIDDEN_CLASS_SUFFIXES =
      List.of("UseCase", "Facade", "Impl", "Adapter", "Port");

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

  @Test
  @DisplayName("should not use use case adapter or port package names")
  void shouldNotUseUseCaseAdapterOrPortPackageNames() {
    long violations =
        CLASSES.stream()
            .map(JavaClass::getPackageName)
            .filter(name -> name.startsWith("com.ai."))
            .filter(LayerPackageNamingRulesTest::hasForbiddenPackageSegment)
            .count();
    assertEquals(0, violations);
  }

  @Test
  @DisplayName("should not use vague class name suffixes")
  void shouldNotUseVagueClassNameSuffixes() {
    long violations =
        CLASSES.stream()
            .filter(javaClass -> javaClass.getPackageName().startsWith("com.ai"))
            .filter(javaClass -> javaClass.getEnclosingClass().isEmpty())
            .map(JavaClass::getSimpleName)
            .filter(LayerPackageNamingRulesTest::hasForbiddenSuffix)
            .count();
    assertEquals(0, violations);
  }

  @Test
  @DisplayName("should not prefix interfaces with I")
  void shouldNotPrefixInterfacesWithI() {
    long violations =
        CLASSES.stream()
            .filter(javaClass -> javaClass.getPackageName().startsWith("com.ai"))
            .filter(JavaClass::isInterface)
            .map(JavaClass::getSimpleName)
            .filter(name -> name.matches("I[A-Z][a-z]\\w*"))
            .count();
    assertEquals(0, violations);
  }

  private static boolean hasForbiddenPackageSegment(String packageName) {
    for (String segment : packageName.split("\\.")) {
      if (FORBIDDEN_PACKAGE_SEGMENTS.contains(segment)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasForbiddenSuffix(String simpleName) {
    return FORBIDDEN_CLASS_SUFFIXES.stream().anyMatch(simpleName::endsWith);
  }

  private static boolean isFeatureModuleClass(JavaClass javaClass) {
    String packageName = javaClass.getPackageName();
    return packageName.startsWith("com.ai.") && !packageName.startsWith("com.ai.common");
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
