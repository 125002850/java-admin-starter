package com.oigit.admin.boot.archunit;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tngtech.archunit.base.DescribedPredicate;
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
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ModuleBoundaryTests {

    private static JavaClasses allClasses;

    private static final DescribedPredicate<JavaClass> CAPABILITY = new DescribedPredicate<>("belong to a business capability") {
        @Override
        public boolean test(JavaClass javaClass) {
            String packageName = javaClass.getPackageName();
            String rootPackage = "com.oigit.admin.";
            if (!packageName.startsWith(rootPackage)) {
                return false;
            }
            String module = packageName.substring(rootPackage.length()).split("\\.", 2)[0];
            return !Set.of("core", "boot").contains(module);
        }
    };

    @BeforeAll
    static void importAllClasses() {
        allClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.oigit.admin");
    }

    @Test
    void admin_core_must_not_depend_on_deleted_admin_mdm() {
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
    void admin_system_packages_must_not_depend_on_admin_boot() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("com.oigit.admin.file..", "com.oigit.admin.dict..", "com.oigit.admin.export..")
                .should().dependOnClassesThat().resideInAPackage("com.oigit.admin.boot..");
        rule.check(allClasses);
    }

    @Test
    void admin_core_must_not_depend_on_admin_iam() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.oigit.admin.core..")
                .should().dependOnClassesThat().resideInAPackage("com.oigit.admin.iam..");
        rule.check(allClasses);
    }

    @Test
    void admin_core_must_not_declare_concrete_mybatis_tables_or_mappers() {
        noClasses()
                .that().resideInAPackage("com.oigit.admin.core..")
                .should().beAnnotatedWith(TableName.class)
                .check(allClasses);
        noClasses()
                .that().resideInAPackage("com.oigit.admin.core..")
                .should().beAssignableTo(BaseMapper.class)
                .check(allClasses);
    }

    @Test
    void admin_system_must_not_depend_on_admin_iam_service_layer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.oigit.admin.file..")
                .or().resideInAPackage("com.oigit.admin.dict..")
                .or().resideInAPackage("com.oigit.admin.export..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.oigit.admin.iam.service..",
                        "com.oigit.admin.iam.infra..",
                        "com.oigit.admin.iam.app..",
                        "com.oigit.admin.iam.domain..",
                        "com.oigit.admin.iam.security.."
                );
        rule.check(allClasses);
    }

    @Test
    void admin_iam_must_not_depend_on_admin_mdm_or_system_implementations() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.oigit.admin.iam..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.oigit.admin.mdm..",
                        "com.oigit.admin.file..",
                        "com.oigit.admin.dict..",
                        "com.oigit.admin.export.."
                );
        rule.check(allClasses);
    }

    @Test
    void admin_iam_controller_must_not_depend_on_service_mapper_or_entity() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.oigit.admin.iam.controller..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.oigit.admin.iam.service..",
                        "com.oigit.admin.iam.infra.mapper..",
                        "com.oigit.admin.iam.infra.entity.."
                );
        rule.check(allClasses);
    }

    @Test
    void no_code_must_depend_on_deleted_admin_system() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAPackage("com.oigit.admin.system..");
        rule.check(allClasses);
    }

    @Test
    void no_code_must_depend_on_deleted_admin_mdm() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAPackage("com.oigit.admin.mdm..");
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
                        "org.apache.ibatis.."
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
    void admin_system_packages_must_not_depend_on_deleted_admin_mdm() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("com.oigit.admin.file..", "com.oigit.admin.dict..", "com.oigit.admin.export..")
                .should().dependOnClassesThat().resideInAPackage("com.oigit.admin.mdm..");
        rule.check(allClasses);
    }

    @Test
    void admin_core_must_not_depend_on_admin_system_implementation_packages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.oigit.admin.core..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.oigit.admin.file..",
                        "com.oigit.admin.dict..",
                        "com.oigit.admin.export.."
                );
        rule.check(allClasses);
    }

    @Test
    void capability_controllers_must_not_bypass_application_layer() {
        ArchRule rule = noClasses()
                .that(CAPABILITY).and().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..domain..",
                        "..infra..",
                        "com.baomidou..",
                        "org.apache.ibatis.."
                );
        rule.check(allClasses);
    }

    @Test
    void dict_request_and_response_dtos_must_use_directional_subpackages() {
        classes()
                .that().resideInAPackage("com.oigit.admin.dict..")
                .and().haveSimpleNameEndingWith("ReqDTO")
                .should().resideInAPackage("com.oigit.admin.dict.dto.req..")
                .check(allClasses);

        classes()
                .that().resideInAPackage("com.oigit.admin.dict..")
                .and().haveSimpleNameEndingWith("RspDTO")
                .should().resideInAPackage("com.oigit.admin.dict.dto.rsp..")
                .check(allClasses);

        noClasses()
                .that().resideInAPackage("com.oigit.admin.dict.dto..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.oigit.admin.dict.controller..",
                        "com.oigit.admin.dict.app..",
                        "com.oigit.admin.dict.domain..",
                        "com.oigit.admin.dict.infra.."
                )
                .check(allClasses);
    }

    @Test
    void all_capability_dtos_must_be_siblings_of_controller_and_app() {
        classes()
                .that(CAPABILITY)
                .and().haveSimpleNameEndingWith("ReqDTO")
                .should().resideInAPackage("com.oigit.admin.*.dto.req..")
                .check(allClasses);

        classes()
                .that(CAPABILITY)
                .and().haveSimpleNameEndingWith("RspDTO")
                .should().resideInAPackage("com.oigit.admin.*.dto.rsp..")
                .check(allClasses);

        noClasses()
                .that(CAPABILITY)
                .should().resideInAPackage("..controller.dto..")
                .check(allClasses);

        noClasses()
                .that(CAPABILITY).and().resideInAPackage("..dto..")
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
                .that(CAPABILITY).and().resideInAnyPackage("..app..", "..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infra..");
        rule.check(allClasses);
    }

    @Test
    void capability_domain_must_not_depend_on_framework_or_web_adapter() {
        ArchRule rule = noClasses()
                .that(CAPABILITY).and().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.baomidou..",
                        "org.apache.ibatis..",
                        "org.springframework..",
                        "jakarta.servlet..",
                        "..dto..",
                        "..controller..",
                        "..app..",
                        "..infra.."
                );
        rule.check(allClasses);
    }

    @Test
    void capability_mappers_must_reside_in_infrastructure_persistence() {
        ArchRule rule = classes()
                .that(CAPABILITY).and().areAnnotatedWith(Mapper.class)
                .should().resideInAPackage("..infra.persistence.mapper");
        rule.check(allClasses);
    }

    @Test
    void capability_persistence_services_must_use_mybatis_plus_service_contracts() {
        classes()
                .that(CAPABILITY).and().haveSimpleNameEndingWith("PersistenceService")
                .and().resideInAPackage("..infra.persistence.service")
                .should().beAssignableTo(IService.class)
                .check(allClasses);

        classes()
                .that(CAPABILITY).and().haveSimpleNameEndingWith("PersistenceServiceImpl")
                .and().resideInAPackage("..infra.persistence.service.impl")
                .should().beAssignableTo(ServiceImpl.class)
                .check(allClasses);
    }

    @Test
    void capability_entities_and_mybatis_services_must_stay_in_persistence() {
        classes().that(CAPABILITY).and().areAnnotatedWith(TableName.class)
                .should().resideInAPackage("..infra.persistence.entity..").check(allClasses);
        classes().that(CAPABILITY).and().areAssignableTo(BaseMapper.class)
                .should().resideInAPackage("..infra.persistence.mapper..").check(allClasses);
        classes().that(CAPABILITY).and().areAssignableTo(IService.class)
                .should().resideInAPackage("..infra.persistence.service..").check(allClasses);
    }

    @Test
    void capability_application_layer_must_not_depend_on_persistence_or_web_transport_types() {
        noClasses()
                .that(CAPABILITY).and().resideInAPackage("..app..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.baomidou..",
                        "org.apache.ibatis..",
                        "jakarta.servlet..",
                        "org.springframework.web.multipart.."
                )
                .check(allClasses);
    }

    @Test
    void capabilities_must_not_reintroduce_legacy_top_level_packages() {
        ArchRule rule = noClasses()
                .that(CAPABILITY)
                .should().resideInAnyPackage(
                        "com.oigit.admin.*.service..",
                        "com.oigit.admin.*.query..",
                        "com.oigit.admin.*.config..",
                        "com.oigit.admin.*.security..",
                        "com.oigit.admin.*.export.."
                );
        rule.check(allClasses);
    }

    @Test
    void external_staff_sdk_must_stay_in_staff_infrastructure() {
        noClasses()
                .that().resideOutsideOfPackage("com.oigit.admin.staff.infra..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.oigit.common.."
                )
                .check(allClasses);
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

    static ArchCondition<JavaClass> haveRequestMappingStartingWith(String forbiddenPrefix) {
        return haveRequestMappingMatching("have @RequestMapping under " + forbiddenPrefix,
                path -> isUnder(path, forbiddenPrefix));
    }

    static ArchCondition<JavaClass> haveNonGlobalDictRequestMapping() {
        return haveRequestMappingMatching("have @RequestMapping under /api/system/dict but not /global",
                path -> isUnder(path, "/api/system/dict") && !isUnder(path, "/api/system/dict/global"));
    }

    private static ArchCondition<JavaClass> haveRequestMappingMatching(String description, Predicate<String> matches) {
        return new ArchCondition<>(description) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (String path : requestPaths(javaClass)) {
                    // noClasses() negates this condition: a matching forbidden path must satisfy it.
                    events.add(new SimpleConditionEvent(javaClass, matches.test(path),
                            javaClass.getName() + " exposes " + path));
                }
            }
        };
    }

    private static boolean isUnder(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private static Set<String> requestPaths(JavaClass javaClass) {
        Class<?> type = javaClass.reflect();
        RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class);
        List<String> classPaths = mappingPaths(classMapping);
        Set<String> paths = new LinkedHashSet<>();
        for (var method : ReflectionUtils.getUniqueDeclaredMethods(type)) {
            RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (methodMapping != null) {
                for (String base : classPaths) {
                    for (String endpoint : mappingPaths(methodMapping)) {
                        paths.add(("/" + base + "/" + endpoint).replaceAll("/+", "/"));
                    }
                }
            }
        }
        if (paths.isEmpty() && classMapping != null) {
            classPaths.forEach(path -> paths.add(("/" + path).replaceAll("/+", "/")));
        }
        return paths;
    }

    private static List<String> mappingPaths(RequestMapping mapping) {
        return mapping == null || mapping.path().length == 0 ? List.of("") : List.of(mapping.path());
    }
}
