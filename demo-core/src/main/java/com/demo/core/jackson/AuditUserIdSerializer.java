package com.demo.core.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.demo.core.operator.CacheUserHolder;

import java.io.IOException;

public class AuditUserIdSerializer extends JsonSerializer<Object> {

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        Long longValue;
        if (value instanceof Number) {
            longValue = ((Number) value).longValue();
        } else {
            gen.writeString(String.valueOf(value));
            return;
        }
        if (longValue == 0L) {
            gen.writeNull();
            return;
        }
        String realName = CacheUserHolder.getRealName(longValue);
        if (realName != null) {
            gen.writeString(realName);
        } else {
            gen.writeString(String.valueOf(longValue));
        }
    }
}
