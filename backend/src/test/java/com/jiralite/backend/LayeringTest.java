package com.jiralite.backend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit tests to enforce layering constraints:
 * - controller must not depend on repository
 * - repository must not depend on controller/service
 */
public class LayeringTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
        .importPackages("com.jiralite.backend");

    @Test
    void controllers_should_not_depend_on_repositories() {
        ArchRule rule = noClasses()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..repository..")
            .allowEmptyShould(true);

        rule.check(importedClasses);
    }

    @Test
    void repositories_should_not_depend_on_controllers() {
        ArchRule rule = noClasses()
            .that()
            .resideInAPackage("..repository..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..controller..")
            .allowEmptyShould(true);

        rule.check(importedClasses);
    }

    @Test
    void repositories_should_not_depend_on_services() {
        ArchRule rule = noClasses()
            .that()
            .resideInAPackage("..repository..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..service..")
            .allowEmptyShould(true);

        rule.check(importedClasses);
    }
}


