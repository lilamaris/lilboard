package com.lilamaris.lilboard.board.launcher;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import org.junit.jupiter.api.DisplayName;

@AnalyzeClasses(packages = "com.lilamaris.lilboard")
class ArchitectureTest {

    @ArchTest
    @DisplayName("domain 계층은 application, jpa, launcher 계층과 Spring에 의존하지 않는다")
    void domainDoesNotDependOnOuterLayersOrSpring(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..board.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..board.application..",
                        "..board.jpa..",
                        "..board.launcher..",
                        "org.springframework..")
                .check(classes);
    }

    @ArchTest
    @DisplayName("application 계층은 jpa 어댑터와 launcher 계층에 의존하지 않는다")
    void applicationDoesNotDependOnAdaptersOrLauncher(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..board.application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..board.jpa..", "..board.launcher..")
                .check(classes);
    }

    @ArchTest
    @DisplayName("kernel은 board-service에 의존하지 않는다")
    void kernelDoesNotDependOnBoardService(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..kernel..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..board..")
                .check(classes);
    }
}
