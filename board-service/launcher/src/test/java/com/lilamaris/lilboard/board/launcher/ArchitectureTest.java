package com.lilamaris.lilboard.board.launcher;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.lilamaris.lilboard.board.LauncherApplication;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

@AnalyzeClasses(packages = "com.lilamaris.lilboard")
class ArchitectureTest {

    @ArchTest
    void domainDoesNotDependOnOuterLayersOrSpring(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..board.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..board.application..",
                        "..board.jpa..",
                        "..board.web..",
                        "..board.launcher..",
                        "org.springframework..")
                .as("domain 계층은 application, jpa, web, launcher 계층과 Spring에 의존하지 않는다")
                .check(classes);
    }

    @ArchTest
    void applicationDoesNotDependOnAdaptersOrLauncher(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..board.application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..board.jpa..", "..board.web..", "..board.launcher..")
                .as("application 계층은 jpa·web 어댑터와 launcher 계층에 의존하지 않는다")
                .check(classes);
    }

    @ArchTest
    void innerLayersDoNotDependOnApplicationEntryPoint(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAnyPackage("..board.domain..", "..board.application..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(LauncherApplication.class.getName())
                .as("domain과 application 계층은 서비스 루트의 실행 진입점에 의존하지 않는다")
                .check(classes);
    }

    @ArchTest
    void kernelDoesNotDependOnBoardService(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..kernel..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..board..")
                .as("kernel은 board-service에 의존하지 않는다")
                .check(classes);
    }
}
