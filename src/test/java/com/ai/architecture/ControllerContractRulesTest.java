package com.ai.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaWildcardType;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

class ControllerContractRulesTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.ai");

  @Test
  @DisplayName("should return named records when a controller answers a request")
  void shouldReturnNamedRecordsWhenAControllerAnswersARequest() {
    methods()
        .that()
        .areDeclaredInClassesThat()
        .areAnnotatedWith(RestController.class)
        .and()
        .arePublic()
        .should(
            new ArchCondition<JavaMethod>("return a named type instead of a map or Object") {
              @Override
              public void check(JavaMethod method, ConditionEvents events) {
                JavaType type = method.getReturnType();
                if (isMap(type) || isLoose(type)) {
                  events.add(SimpleConditionEvent.violated(method, method.getFullName()));
                }
              }
            })
        .check(CLASSES);
  }

  @Test
  @DisplayName("should type every dto component when it is part of the API")
  void shouldTypeEveryDtoComponentWhenItIsPartOfTheApi() {
    fields()
        .that()
        .areDeclaredInClassesThat()
        .resideInAPackage("..controller.dto..")
        .and()
        .areNotStatic()
        .should(
            new ArchCondition<JavaField>("not be Object, a wildcard, or contain them") {
              @Override
              public void check(JavaField field, ConditionEvents events) {
                if (isLoose(field.getType())) {
                  events.add(SimpleConditionEvent.violated(field, field.getFullName()));
                }
              }
            })
        .check(CLASSES);
  }

  private static boolean isMap(JavaType type) {
    return type.toErasure().isAssignableTo(Map.class);
  }

  private static boolean isLoose(JavaType type) {
    if (type instanceof JavaWildcardType) {
      return true;
    }
    if (type.toErasure().isEquivalentTo(Object.class)) {
      return true;
    }
    if (type instanceof JavaParameterizedType parameterized) {
      return parameterized.getActualTypeArguments().stream()
          .anyMatch(ControllerContractRulesTest::isLoose);
    }
    return false;
  }
}
