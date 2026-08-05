package com.oigit.admin.core.translation;

import com.oigit.admin.core.enums.BaseEnum;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic two-phase batch translator: scan and deduplicate first, then invoke
 * each provider once and fill target fields. Reflection metadata is cached by
 * class and therefore never rebuilt per row.
 */
public final class TranslationEngine {

    private final Map<String, TranslationProvider> providers;
    private final Map<Class<?>, BeanMetadata> metadataCache = new ConcurrentHashMap<>();

    public TranslationEngine(List<TranslationProvider> providers) {
        Map<String, TranslationProvider> indexed = new LinkedHashMap<>();
        for (TranslationProvider provider : providers) {
            TranslationProvider previous = indexed.putIfAbsent(provider.type(), provider);
            if (previous != null) {
                throw new IllegalStateException("duplicate translation provider: " + provider.type());
            }
        }
        this.providers = Map.copyOf(indexed);
    }

    public <T> T translate(T root, TranslationScene scene) {
        if (root == null) {
            return null;
        }
        List<TranslationSlot> slots = new ArrayList<>();
        scan(root, scene, slots, java.util.Collections.newSetFromMap(new IdentityHashMap<>()));

        Map<String, LinkedHashSet<TranslationKey>> keysByType = new LinkedHashMap<>();
        for (TranslationSlot slot : slots) {
            keysByType.computeIfAbsent(slot.annotation().type(), ignored -> new LinkedHashSet<>())
                    .add(slot.key());
        }

        Map<String, Map<TranslationKey, String>> valuesByType = new LinkedHashMap<>();
        keysByType.forEach((type, keys) -> {
            TranslationProvider provider = providers.get(type);
            if (provider == null) {
                throw new IllegalStateException("translation provider not found: " + type);
            }
            Map<TranslationKey, String> translated = provider.translate(Set.copyOf(keys));
            valuesByType.put(type, translated == null ? Map.of() : translated);
        });

        for (TranslationSlot slot : slots) {
            String value = valuesByType.getOrDefault(slot.annotation().type(), Map.of()).get(slot.key());
            if (value == null && slot.annotation().missing() == MissingTranslationPolicy.SOURCE) {
                value = slot.key().value();
            }
            ReflectionUtils.setField(slot.targetField(), slot.bean(), value);
        }
        return root;
    }

    private void scan(Object value, TranslationScene scene, List<TranslationSlot> slots, Set<Object> visited) {
        if (value == null || isSimple(value.getClass()) || !visited.add(value)) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            map.values().forEach(item -> scan(item, scene, slots, visited));
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(item -> scan(item, scene, slots, visited));
            return;
        }
        if (value.getClass().isArray()) {
            for (int index = 0; index < Array.getLength(value); index++) {
                scan(Array.get(value, index), scene, slots, visited);
            }
            return;
        }
        if (!value.getClass().getPackageName().startsWith("com.oigit.admin")) {
            return;
        }

        BeanMetadata metadata = metadataCache.computeIfAbsent(value.getClass(), this::inspect);
        for (TranslatedField translatedField : metadata.translatedFields()) {
            Translate annotation = translatedField.annotation();
            if (!supports(annotation, scene)) {
                continue;
            }
            Object source = ReflectionUtils.getField(translatedField.sourceField(), value);
            String normalized = normalizeSource(source);
            if (StringUtils.hasText(normalized)) {
                slots.add(new TranslationSlot(
                        value,
                        translatedField.targetField(),
                        annotation,
                        new TranslationKey(annotation.qualifier(), normalized)
                ));
            }
        }
        for (Field field : metadata.nestedFields()) {
            scan(ReflectionUtils.getField(field, value), scene, slots, visited);
        }
    }

    private BeanMetadata inspect(Class<?> type) {
        List<Field> allFields = allFields(type);
        Map<String, Field> fieldsByName = new LinkedHashMap<>();
        allFields.forEach(field -> fieldsByName.putIfAbsent(field.getName(), field));

        List<TranslatedField> translatedFields = new ArrayList<>();
        for (Field sourceField : allFields) {
            Translate annotation = sourceField.getAnnotation(Translate.class);
            if (annotation == null) {
                continue;
            }
            Field targetField = fieldsByName.get(annotation.targetField());
            if (targetField == null) {
                throw new IllegalStateException(type.getName() + " translation target field not found: " + annotation.targetField());
            }
            if (!String.class.equals(targetField.getType())) {
                throw new IllegalStateException(type.getName() + " translation target must be String: " + annotation.targetField());
            }
            ReflectionUtils.makeAccessible(sourceField);
            ReflectionUtils.makeAccessible(targetField);
            translatedFields.add(new TranslatedField(sourceField, targetField, annotation));
        }

        List<Field> nestedFields = allFields.stream()
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .filter(field -> !isSimple(field.getType()))
                .peek(ReflectionUtils::makeAccessible)
                .toList();
        return new BeanMetadata(List.copyOf(translatedFields), nestedFields);
    }

    private List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.isSynthetic()) {
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private boolean supports(Translate annotation, TranslationScene scene) {
        for (TranslationScene supported : annotation.scenes()) {
            if (supported == scene) {
                return true;
            }
        }
        return false;
    }

    private String normalizeSource(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof BaseEnum baseEnum) {
            return baseEnum.getCode();
        }
        return String.valueOf(source);
    }

    private boolean isSimple(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || CharSequence.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type)
                || Boolean.class.equals(type)
                || Character.class.equals(type)
                || Temporal.class.isAssignableFrom(type)
                || UUID.class.equals(type)
                || Class.class.equals(type);
    }

    private record BeanMetadata(List<TranslatedField> translatedFields, List<Field> nestedFields) {
    }

    private record TranslatedField(Field sourceField, Field targetField, Translate annotation) {
    }

    private record TranslationSlot(Object bean, Field targetField, Translate annotation, TranslationKey key) {
    }
}
