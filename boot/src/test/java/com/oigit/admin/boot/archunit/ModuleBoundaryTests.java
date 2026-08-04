package com.oigit.admin.boot.archunit;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import org.apache.ibatis.annotations.Mapper;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ModuleBoundaryTests {

    private static JavaClasses allClasses;

    @BeforeAll
    static void importAllClasses() {
        allClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.oigit.admin");
    }

    @Test
    void admin_core_must_not_depend_on_admin_mdm() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.oigit.admin.core..")
                .should().dependOnClassesThat().resideInAPackage("com.oigit.admin.mdm..");
        rule.check(allClasses);
    }

    @Test
    void admin_core_must_not_depend_on_admin_boot() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.oigit.admin.core..")
                .should().dependOnClassesThat().resideInAPackage("com.oigit.admin.boot..");
        rule.check(allClasses);
    }

    @Test
    void admin_mdm_must_not_depend_on_admin_boot() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.oigit.admin.mdm..")
                .should().dependOnClassesThat().resideInAPackage("com.oigit.admin.boot..")
                .allowEmptyShould(true);
        rule.check(allClasses);
    }

    @Test
    void no_code_must_depend_on_deleted_admin_system() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAPackage("com.oigit.admin.system..");
        rule.check(allClasses);
    }

    @Test
    void no_code_must_depend_on_deleted_tenant_package() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAPackage("com.oigit.admin.core.tenant..");
        rule.check(allClasses);
    }

    @Test
    void no_code_must_depend_on_postloan_package() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAPackage("com.oigit.admin.postloan..");
        rule.check(allClasses);
    }

    @Test
    void operator_context_and_filter_must_exist() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.oigit.admin.core.operator")
                .should().bePublic();
        rule.check(allClasses);
    }

    @Test
    void core_operator_port_must_not_own_user_cache_persistence() {
        noClasses()
                .that().resideInAPackage("com.oigit.admin.core.operator..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.baomidou..",
                        "org.apache.ibatis..",
                        "com.oigit.admin.staff.infra.."
                )
                .check(allClasses);

        noClasses()
                .that().resideInAPackage("com.oigit.admin.core..")
                .should().haveSimpleName("CacheUserEntity")
                .orShould().haveSimpleName("CacheUserMapper")
                .orShould().haveSimpleName("CacheUserService")
                .check(allClasses);
    }

    @Test
    void common_meta_object_handler_must_exist() {
        ArchRule rule = classes()
                .that().haveSimpleName("CommonMetaObjectHandler")
                .should().resideInAPackage("com.oigit.admin.core.mybatis");
        rule.check(allClasses);
    }

    @Test
    void admin_system_file_package_must_not_depend_on_admin_boot() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.oigit.admin.file..")
                .should().dependOnClassesThat().resideInAPackage("com.oigit.admin.boot..");
        rule.check(allClasses);
    }

    @Test
    void admin_system_file_package_must_not_depend_on_admin_mdm() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.oigit.admin.file..")
                .should().dependOnClassesThat().resideInAPackage("com.oigit.admin.mdm..");
        rule.check(allClasses);
    }

    @Test
    void admin_core_must_not_depend_on_admin_system_file_package() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.oigit.admin.core..")
                .should().dependOnClassesThat().resideInAPackage("com.oigit.admin.file..");
        rule.check(allClasses);
    }

    @Test
    void admin_mdm_must_not_depend_on_admin_system_file_package() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.oigit.admin.mdm..")
                .should().dependOnClassesThat().resideInAPackage("com.oigit.admin.file..")
                .allowEmptyShould(true);
        rule.check(allClasses);
    }

    @Test
    void capability_controllers_must_not_bypass_application_layer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..domain..",
                        "..infra.."
                );
        rule.check(allClasses);
    }

    @Test
    void all_capability_dtos_must_be_siblings_of_controller_and_app() {
        classes()
                .that().resideInAnyPackage(
                        "com.oigit.admin.dict..",
                        "com.oigit.admin.export..",
                        "com.oigit.admin.file..",
                        "com.oigit.admin.staff.."
                )
                .and().haveSimpleNameEndingWith("ReqDTO")
                .should().resideInAnyPackage(
                        "com.oigit.admin.dict.dto.req..",
                        "com.oigit.admin.export.dto.req..",
                        "com.oigit.admin.file.dto.req..",
                        "com.oigit.admin.staff.dto.req.."
                )
                .check(allClasses);

        classes()
                .that().resideInAnyPackage(
                        "com.oigit.admin.dict..",
                        "com.oigit.admin.export..",
                        "com.oigit.admin.file..",
                        "com.oigit.admin.staff.."
                )
                .and().haveSimpleNameEndingWith("RspDTO")
                .should().resideInAnyPackage(
                        "com.oigit.admin.dict.dto.rsp..",
                        "com.oigit.admin.export.dto.rsp..",
                        "com.oigit.admin.file.dto.rsp..",
                        "com.oigit.admin.staff.dto.rsp.."
                )
                .check(allClasses);

        noClasses()
                .should().resideInAnyPackage(
                        "com.oigit.admin.dict.controller.dto..",
                        "com.oigit.admin.export.controller.dto..",
                        "com.oigit.admin.file.controller.dto..",
                        "com.oigit.admin.staff.controller.dto.."
                )
                .check(allClasses);

        noClasses()
                .that().resideInAnyPackage(
                        "com.oigit.admin.dict.dto..",
                        "com.oigit.admin.export.dto..",
                        "com.oigit.admin.file.dto..",
                        "com.oigit.admin.staff.dto.."
                )
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..controller..",
                        "..app..",
                        "..domain..",
                        "..infra.."
                )
                .check(allClasses);
    }

    @Test
    void capability_application_and_domain_must_not_depend_on_infrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(
                        "..app..",
                        "..domain.."
                )
                .should().dependOnClassesThat().resideInAPackage("..infra..");
        rule.check(allClasses);
    }

    @Test
    void capability_domain_must_not_depend_on_framework_or_web_adapter() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.baomidou..",
                        "org.apache.ibatis..",
                        "org.springframework..",
                        "..controller..",
                        "..app..",
                        "..infra.."
                );
        rule.check(allClasses);
    }

    @Test
    void capability_mappers_must_reside_in_infrastructure_persistence() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Mapper.class)
                .should().resideInAPackage("..infra.persistence.mapper");
        rule.check(allClasses);
    }

    @Test
    void capability_persistence_services_must_use_mybatis_plus_service_contracts() {
        classes()
                .that().haveSimpleNameEndingWith("PersistenceService")
                .and().resideInAPackage("..infra.persistence.service")
                .should().beAssignableTo(IService.class)
                .check(allClasses);

        classes()
                .that().haveSimpleNameEndingWith("PersistenceServiceImpl")
                .and().resideInAPackage("..infra.persistence.service.impl")
                .should().beAssignableTo(ServiceImpl.class)
                .check(allClasses);
    }

    @Test
    void application_layer_must_not_depend_on_persistence_or_web_transport_types() {
        noClasses()
                .that().resideInAPackage("..app..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.baomidou..",
                        "org.apache.ibatis..",
                        "org.springframework.web.multipart.."
                )
                .check(allClasses);
    }

    @Test
    void capabilities_must_not_reintroduce_legacy_top_level_packages() {
        ArchRule rule = noClasses()
                .should().resideInAnyPackage(
                        "com.oigit.admin.dict.service..",
                        "com.oigit.admin.dict.query..",
                        "com.oigit.admin.dict.config..",
                        "com.oigit.admin.dict.export..",
                        "com.oigit.admin.export.service..",
                        "com.oigit.admin.export.query..",
                        "com.oigit.admin.export.config..",
                        "com.oigit.admin.file.service..",
                        "com.oigit.admin.file.query..",
                        "com.oigit.admin.file.config..",
                        "com.oigit.admin.file.export..",
                        "com.oigit.admin.staff.service..",
                        "com.oigit.admin.staff.query..",
                        "com.oigit.admin.staff.config.."
                );
        rule.check(allClasses);
    }

    @Test
    void external_staff_sdk_must_stay_in_staff_infrastructure() {
        noClasses()
                .that().resideOutsideOfPackage("com.oigit.admin.staff.infra..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.oigit.appcik..",
                        "com.oigit.common.."
                )
                .check(allClasses);
    }

    @Test
    void no_controller_must_expose_api_system_paths() {
        ArchRule rule = noClasses()
                .should(haveForbiddenRequestMappingStartingWith("/api/system", "/api/system/dict"));
        rule.check(allClasses);
    }

    @Test
    void no_controller_must_expose_non_global_dict_paths() {
        ArchRule rule = noClasses()
                .should(haveNonGlobalDictRequestMapping());
        rule.check(allClasses);
    }

    @Test
    void no_controller_must_expose_vendor_qiniu_paths() {
        ArchRule rule = noClasses()
                .should(haveRequestMappingStartingWith("/api/framework/qiniu"));
        rule.check(allClasses);
    }

    @Test
    void no_controller_must_expose_postloan_paths() {
        ArchRule rule = noClasses()
                .should(haveRequestMappingStartingWith("/api/postloan"));
        rule.check(allClasses);
    }

    private static ArchCondition<JavaClass> haveForbiddenRequestMappingStartingWith(String forbiddenPrefix, String... allowedPrefixes) {
        return new ArchCondition<>("have @RequestMapping starting with " + forbiddenPrefix + " (excluding allowed prefixes)") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.tryGetAnnotationOfType(RequestMapping.class).ifPresent(rm -> {
                    for (String path : rm.value()) {
                        if (!path.startsWith(forbiddenPrefix)) {
                            continue;
                        }
                        boolean allowed = false;
                        for (String allowedPrefix : allowedPrefixes) {
                            if (path.startsWith(allowedPrefix)) {
                                allowed = true;
                                break;
                            }
                        }
                        if (!allowed) {
                            events.add(SimpleConditionEvent.violated(javaClass,
                                    javaClass.getName() + " exposes " + path));
                        }
                    }
                });
            }
        };
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
        return new ArchCondition<>("have @RequestMapping under /api/system/dict but not /global") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.tryGetAnnotationOfType(RequestMapping.class).ifPresent(rm -> {
                    for (String path : rm.value()) {
                        if (path.startsWith("/api/system/dict")
                                && !path.startsWith("/api/system/dict/global")) {
                            events.add(SimpleConditionEvent.violated(javaClass,
                                    javaClass.getName() + " exposes non-global dict path " + path));
                        }
                    }
                });
            }
        };
    }
}
