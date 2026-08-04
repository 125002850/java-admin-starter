package com.oigit.admin.dict.infra.config;

import com.oigit.admin.core.enums.BaseEnum;
import com.oigit.admin.core.enums.DictionaryEnum;
import com.oigit.admin.dict.domain.service.EnumDictionaryPolicy;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Discovers every @DictionaryEnum once at startup; no per-request classpath scanning. */
@Component
public class ClasspathEnumDictionaryPolicy implements EnumDictionaryPolicy {

    private final Map<String, Set<String>> codesByType;

    public ClasspathEnumDictionaryPolicy() {
        this.codesByType = discover();
    }

    @Override
    public boolean isEnumDictionary(String dictTypeCode) {
        return codesByType.containsKey(dictTypeCode);
    }

    @Override
    public Set<String> expectedCodes(String dictTypeCode) {
        return codesByType.getOrDefault(dictTypeCode, Set.of());
    }

    public Map<String, Set<String>> contracts() {
        return codesByType;
    }

    private Map<String, Set<String>> discover() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(DictionaryEnum.class));
        Map<String, Set<String>> discovered = new LinkedHashMap<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents("com.oigit.admin")) {
            Class<?> type = load(candidate);
            if (type == null || !type.isEnum() || !BaseEnum.class.isAssignableFrom(type)) {
                continue;
            }
            DictionaryEnum annotation = type.getAnnotation(DictionaryEnum.class);
            LinkedHashSet<String> codes = new LinkedHashSet<>();
            for (Object constant : type.getEnumConstants()) {
                codes.add(((BaseEnum) constant).getCode());
            }
            Set<String> previous = discovered.putIfAbsent(annotation.value(), Set.copyOf(codes));
            if (previous != null && !previous.equals(codes)) {
                throw new IllegalStateException("conflicting enum dictionary contract: " + annotation.value());
            }
        }
        return Map.copyOf(discovered);
    }

    private Class<?> load(BeanDefinition candidate) {
        try {
            return ClassUtils.forName(candidate.getBeanClassName(), getClass().getClassLoader());
        } catch (ClassNotFoundException | LinkageError ignored) {
            return null;
        }
    }
}
