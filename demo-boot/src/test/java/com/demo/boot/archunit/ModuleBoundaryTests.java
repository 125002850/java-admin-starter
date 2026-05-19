package com.demo.boot.archunit;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ModuleBoundaryTests {

    private static JavaClasses allClasses;

    @BeforeAll
    static void importAllClasses() {
        allClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.demo");
    }

    @Test
    void demo_core_must_not_depend_on_demo_mdm() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.demo.core..")
                .should().dependOnClassesThat().resideInAPackage("com.demo.mdm..");
        rule.check(allClasses);
    }

    @Test
    void demo_core_must_not_depend_on_demo_boot() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.demo.core..")
                .should().dependOnClassesThat().resideInAPackage("com.demo.boot..");
        rule.check(allClasses);
    }

    @Test
    void demo_mdm_must_not_depend_on_demo_boot() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.demo.mdm..")
                .should().dependOnClassesThat().resideInAPackage("com.demo.boot..");
        rule.check(allClasses);
    }

    @Test
    void no_code_must_depend_on_deleted_demo_system() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAPackage("com.demo.system..");
        rule.check(allClasses);
    }

    @Test
    void no_code_must_depend_on_deleted_tenant_package() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAPackage("com.demo.core.tenant..");
        rule.check(allClasses);
    }

    @Test
    void operator_context_and_filter_must_exist() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.demo.core.operator")
                .should().bePublic();
        rule.check(allClasses);
    }

    @Test
    void common_meta_object_handler_must_exist() {
        ArchRule rule = classes()
                .that().haveSimpleName("CommonMetaObjectHandler")
                .should().resideInAPackage("com.demo.core.mybatis");
        rule.check(allClasses);
    }

    @Test
    void no_controller_must_expose_api_system_paths() {
        ArchRule rule = noClasses()
                .should(haveRequestMappingStartingWith("/api/system"));
        rule.check(allClasses);
    }

    @Test
    void no_controller_must_expose_non_global_dict_paths() {
        ArchRule rule = noClasses()
                .should(haveNonGlobalDictRequestMapping());
        rule.check(allClasses);
    }

    private static ArchCondition<JavaClass> haveRequestMappingStartingWith(String forbiddenPrefix) {
        return new ArchCondition<>("have @RequestMapping starting with " + forbiddenPrefix) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.tryGetAnnotationOfType(RequestMapping.class).ifPresent(rm -> {
                    for (String path : rm.value()) {
                        if (path.startsWith(forbiddenPrefix)) {
                            events.add(SimpleConditionEvent.violated(javaClass,
                                    javaClass.getName() + " exposes " + path));
                        }
                    }
                });
            }
        };
    }

    private static ArchCondition<JavaClass> haveNonGlobalDictRequestMapping() {
        return new ArchCondition<>("have @RequestMapping under /api/mdm/dict but not /global") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.tryGetAnnotationOfType(RequestMapping.class).ifPresent(rm -> {
                    for (String path : rm.value()) {
                        if (path.startsWith("/api/mdm/dict")
                                && !path.startsWith("/api/mdm/dict/global")) {
                            events.add(SimpleConditionEvent.violated(javaClass,
                                    javaClass.getName() + " exposes non-global dict path " + path));
                        }
                    }
                });
            }
        };
    }
}
