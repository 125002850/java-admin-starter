package com.oigit.admin.boot.config;

import com.fasterxml.jackson.databind.JavaType;
import com.oigit.admin.core.enums.BaseEnum;
import com.oigit.admin.core.enums.DictionaryEnum;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps enum wire values as strings while preserving field-level schema metadata. */
@Component
public class EnumModelConverter implements ModelConverter {

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Schema<?> resolve(io.swagger.v3.core.converter.AnnotatedType type,
                             ModelConverterContext context, Iterator<ModelConverter> chain) {
        Class<?> rawType = type.getType() instanceof Class<?> clazz ? clazz
                : type.getType() instanceof JavaType javaType ? javaType.getRawClass() : null;
        if (rawType == null || !rawType.isEnum()) {
            return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        }
        boolean baseEnum = BaseEnum.class.isAssignableFrom(rawType);
        Method codeGetter = stringGetter(rawType, "getCode");
        Method labelGetter = stringGetter(rawType, "getLabel");
        if (!baseEnum && (codeGetter == null || labelGetter == null)) {
            return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        }
        List<String> codes = new ArrayList<>();
        Map<String, String> labels = new LinkedHashMap<>();
        for (Object constant : rawType.getEnumConstants()) {
            if (baseEnum) {
                codes.add(((BaseEnum) constant).getCode());
            } else {
                String code = invoke(constant, codeGetter);
                codes.add(code);
                labels.put(code, invoke(constant, labelGetter));
            }
        }
        Schema schema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        if (schema == null) schema = new StringSchema();
        schema.set$ref(null);
        schema.setType("string");
        schema.setFormat(null);
        schema.setEnum(codes);
        DictionaryEnum dictionary = rawType.getAnnotation(DictionaryEnum.class);
        if (dictionary != null) schema.addExtension("x-dict-type", dictionary.value());
        if (!labels.isEmpty()) schema.addExtension("x-enum-labels", labels);
        return schema;
    }

    private static Method stringGetter(Class<?> type, String name) {
        try {
            Method method = type.getMethod(name);
            return method.getReturnType() == String.class ? method : null;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static String invoke(Object constant, Method method) {
        try {
            return (String) method.invoke(constant);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read enum metadata: " + method, exception);
        }
    }
}
