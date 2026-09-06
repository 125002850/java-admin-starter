package com.oigit.admin.boot.archunit;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

class RequestMappingRuleTests {

    @Test
    void rejectsForbiddenControllerPrefix() {
        assertThat(forbiddenPrefixViolation(PostloanController.class, "/api/postloan")).isTrue();
    }

    @Test
    void checksRequestMappingPathAlias() {
        assertThat(forbiddenPrefixViolation(QiniuController.class, "/api/framework/qiniu")).isTrue();
    }

    @Test
    void checksCombinedControllerAndMethodPaths() {
        assertThat(forbiddenPrefixViolation(MethodMappedController.class, "/api/postloan")).isTrue();
    }

    @Test
    void checksMethodMappingsWithoutControllerMapping() {
        assertThat(forbiddenPrefixViolation(MethodOnlyController.class, "/api/postloan")).isTrue();
    }

    @Test
    void allowsDifferentPathSegmentWithSimilarPrefix() {
        assertThat(forbiddenPrefixViolation(SimilarPrefixController.class, "/api/postloan")).isFalse();
    }

    @Test
    void rejectsNonGlobalDictionaryPaths() {
        assertThat(nonGlobalDictViolation(PrivateDictController.class)).isTrue();
        assertThat(nonGlobalDictViolation(SimilarGlobalDictController.class)).isTrue();
    }

    @Test
    void acceptsGlobalDictionaryAndUnrelatedControllers() {
        assertThat(nonGlobalDictViolation(GlobalDictController.class)).isFalse();
        assertThat(nonGlobalDictViolation(PostloanController.class)).isFalse();
        assertThat(forbiddenPrefixViolation(GlobalDictController.class, "/api/postloan")).isFalse();
    }

    @Test
    void acceptsGlobalDictionaryPathAssembledAtMethodLevel() {
        assertThat(nonGlobalDictViolation(MethodGlobalDictController.class)).isFalse();
    }

    private boolean forbiddenPrefixViolation(Class<?> controller, String prefix) {
        return noClasses().should(ModuleBoundaryTests.haveRequestMappingStartingWith(prefix))
                .evaluate(new ClassFileImporter().importClasses(controller)).hasViolation();
    }

    private boolean nonGlobalDictViolation(Class<?> controller) {
        return noClasses().should(ModuleBoundaryTests.haveNonGlobalDictRequestMapping())
                .evaluate(new ClassFileImporter().importClasses(controller)).hasViolation();
    }

    @RequestMapping("/api/postloan/cases")
    static class PostloanController { }

    @RequestMapping(path = "/api/framework/qiniu/files")
    static class QiniuController { }

    @RequestMapping("/api")
    static class MethodMappedController {
        @PostMapping(path = "/postloan/cases")
        void list() { }
    }

    static class MethodOnlyController {
        @PostMapping("/api/postloan/cases")
        void list() { }
    }

    @RequestMapping("/api/postloaner")
    static class SimilarPrefixController { }

    @RequestMapping("/api/system/dict/private")
    static class PrivateDictController { }

    @RequestMapping("/api/system/dict/globalized")
    static class SimilarGlobalDictController { }

    @RequestMapping(path = "/api/system/dict/global/items")
    static class GlobalDictController { }

    @RequestMapping("/api/system/dict")
    static class MethodGlobalDictController {
        @PostMapping("/global/items")
        void list() { }
    }
}
