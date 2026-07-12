package com.ai.common;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Architecture rules")
class ArchitectureRulesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .importPackages("com.ai");

    @Test
    @DisplayName("should forbid legacy domain port packages")
    void shouldForbidLegacyDomainPortPackages() {
        ArchRuleDefinition.noClasses()
                .should().resideInAPackage("..domain.port..")
                .check(CLASSES);
    }

    @Test
    @DisplayName("should forbid hexagonal adapter in/out packages")
    void shouldForbidHexagonalAdapterPackages() {
        ArchRuleDefinition.noClasses()
                .should().resideInAnyPackage("..adapter.in..", "..adapter.out..")
                .check(CLASSES);
    }
}
