package com.demo.core.mybatis;

import com.demo.core.operator.OperatorContext;
import org.apache.ibatis.reflection.MetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonMetaObjectHandlerTests {

    private static final Long OPERATOR_ID = 42L;

    private final CommonMetaObjectHandler handler = new CommonMetaObjectHandler();

    @Mock
    private MetaObject metaObject;

    @BeforeEach
    void setUp() {
        OperatorContext.clear();
    }

    @AfterEach
    void tearDown() {
        OperatorContext.clear();
    }

    @Test
    void insertFill_should_use_operator_id_when_context_is_populated() {
        OperatorContext.set(OPERATOR_ID, "test-operator");
        when(metaObject.hasSetter(any())).thenReturn(true);

        handler.insertFill(metaObject);

        verify(metaObject).setValue(eq("createBy"), eq(OPERATOR_ID));
        verify(metaObject).setValue(eq("updateBy"), eq(OPERATOR_ID));
    }

    @Test
    void insertFill_should_fallback_to_zero_when_operator_context_is_empty() {
        when(metaObject.hasSetter(any())).thenReturn(true);

        handler.insertFill(metaObject);

        verify(metaObject).setValue(eq("createBy"), eq(0L));
        verify(metaObject).setValue(eq("updateBy"), eq(0L));
    }

    @Test
    void updateFill_should_use_operator_id_when_context_is_populated() {
        OperatorContext.set(OPERATOR_ID, "test-operator");
        when(metaObject.hasSetter(any())).thenReturn(true);

        handler.updateFill(metaObject);

        verify(metaObject).setValue(eq("updateBy"), eq(OPERATOR_ID));
        verify(metaObject).setValue(eq("updateTime"), any(LocalDateTime.class));
    }

    @Test
    void updateFill_should_fallback_to_zero_when_operator_context_is_empty() {
        when(metaObject.hasSetter(any())).thenReturn(true);

        handler.updateFill(metaObject);

        verify(metaObject).setValue(eq("updateBy"), eq(0L));
    }

    @Test
    void updateFill_should_skip_update_by_when_no_setter() {
        when(metaObject.hasSetter("updateTime")).thenReturn(false);
        when(metaObject.hasSetter("updateBy")).thenReturn(false);

        handler.updateFill(metaObject);

        verify(metaObject, never()).setValue(eq("updateBy"), any());
    }
}
