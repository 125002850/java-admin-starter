package com.oigit.admin.boot.config;

import com.oigit.admin.core.enums.BaseEnum;
import com.oigit.admin.core.enums.DictionaryEnum;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Arrays;

@Component
public class EnumModelConverter implements ModelConverter {

    @Override
    public Schema<?> resolve(io.swagger.v3.core.converter.AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (type.getType() instanceof Class<?> rawType && rawType.isEnum() && BaseEnum.class.isAssignableFrom(rawType)) {
            Schema<String> schema = new Schema<>();
            schema.setType("string");
            schema.setEnum(Arrays.stream(rawType.getEnumConstants())
                    .map(BaseEnum.class::cast)
                    .map(BaseEnum::getCode)
                    .toList());
            DictionaryEnum dictionaryEnum = rawType.getAnnotation(DictionaryEnum.class);
            if (dictionaryEnum != null) {
                schema.addExtension("x-dict-type", dictionaryEnum.value());
            }
            return schema;
        }
        if (chain.hasNext()) {
            return chain.next().resolve(type, context, chain);
        }
        return null;
    }
}
