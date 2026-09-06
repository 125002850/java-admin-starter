package com.oigit.admin.iam.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import org.junit.jupiter.api.Test;

class IamLayeringTests {

    private final JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.oigit.admin.iam");

    @Test
    void applicationAndDomainMustNotDependOnPersistenceOrInfrastructure() {
        noClasses()
                .that()
                .resideInAnyPackage("..iam.app..", "..iam.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..iam.infra..",
                        "..iam.service..",
                        "..iam.security..",
                        "..iam.config..",
                        "com.baomidou.mybatisplus..",
                        "org.apache.ibatis..")
                .check(classes);
    }

    @Test
    void domainMustRemainIndependentOfWebAndFrameworkAdapters() {
        noClasses()
                .that()
                .resideInAPackage("..iam.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..iam.dto..",
                        "..iam.app..",
                        "..iam.controller..",
                        "..iam.infra..",
                        "org.springframework..",
                        "jakarta.servlet..",
                        "com.fasterxml.jackson..",
                        "com.baomidou.mybatisplus..",
                        "org.apache.ibatis..",
                        "io.swagger..",
                        "com.oigit.admin.core.web..",
                        "com.oigit.admin.core.mybatis..")
                .check(classes);
    }

    @Test
    void controllersMustReachBusinessLogicThroughApplicationServices() {
        noClasses()
                .that()
                .resideInAPackage("..iam.controller..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..iam.domain..", "..iam.infra..", "..iam.service..", "..iam.security..")
                .check(classes);
    }

    @Test
    void persistenceObjectsMustLiveInsidePersistenceAdapters() {
        classes()
                .that()
                .haveSimpleNameEndingWith("Entity")
                .should()
                .resideInAPackage("..iam.infra.persistence.entity..")
                .check(classes);
        classes()
                .that()
                .haveSimpleNameEndingWith("Mapper")
                .and()
                .resideOutsideOfPackage("..iam.app..")
                .should()
                .resideInAPackage("..iam.infra.persistence.mapper..")
                .check(classes);
    }

    @Test
    void webDtosMustBeSeparatedByRequestAndResponse() {
        classes()
                .that()
                .haveSimpleNameEndingWith("ReqDTO")
                .should()
                .resideInAPackage("..iam.dto.req..")
                .check(classes);
        classes()
                .that()
                .haveSimpleNameEndingWith("RspDTO")
                .should()
                .resideInAPackage("..iam.dto.rsp..")
                .check(classes);
    }

    @Test
    void webDtosMustNotExposeInternalModels() {
        noClasses()
                .that()
                .resideInAPackage("..iam.dto..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..iam.domain..", "..iam.app..", "..iam.controller..", "..iam.infra..")
                .check(classes);
    }

    @Test
    void myBatisServicesMustStayInsidePersistenceAdapters() {
        classes()
                .that()
                .areAssignableTo(com.baomidou.mybatisplus.extension.service.IService.class)
                .should()
                .resideInAPackage("..iam.infra.persistence.service..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void obsoleteMixedPackagesMustNotReturn() {
        noClasses()
                .should()
                .resideInAnyPackage(
                        "..iam.service..",
                        "..iam.security..",
                        "..iam.config..",
                        "..iam.event..",
                        "..iam.infra.entity..",
                        "..iam.infra.mapper..")
                .check(classes);
    }
}
