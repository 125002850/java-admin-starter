package com.demo.boot.config;

import com.demo.core.enums.BaseEnum;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.stereotype.Component;

import java.util.Iterator;

@Component
public class EnumModelConverter implements ModelConverter {

    private static final String ENUM_VO_REF = "#/components/schemas/EnumVO";

    @Override
    public Schema<?> resolve(io.swagger.v3.core.converter.AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (type.getType() instanceof Class<?> rawType && rawType.isEnum() && BaseEnum.class.isAssignableFrom(rawType)) {
            return new Schema<>().$ref(ENUM_VO_REF);
        }
        if (chain.hasNext()) {
            return chain.next().resolve(type, context, chain);
        }
        return null;
    }
}
