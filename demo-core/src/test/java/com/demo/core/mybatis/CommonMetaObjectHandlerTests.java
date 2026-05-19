package com.demo.core.mybatis;

import com.demo.core.operator.OperatorContext;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CommonMetaObjectHandlerTests {

    private static final Long OPERATOR_ID = 42L;

    private final CommonMetaObjectHandler handler = new CommonMetaObjectHandler();

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
        TestEntity entity = new TestEntity();
        MetaObject metaObject = metaObjectFor(entity);

        handler.insertFill(metaObject);

        assertThat(entity.createBy).isEqualTo(OPERATOR_ID);
        assertThat(entity.updateBy).isEqualTo(OPERATOR_ID);
        assertThat(entity.createTime).isNotNull();
        assertThat(entity.updateTime).isNotNull();
    }

    @Test
    void insertFill_should_fallback_to_zero_when_operator_context_is_empty() {
        TestEntity entity = new TestEntity();
        MetaObject metaObject = metaObjectFor(entity);

        handler.insertFill(metaObject);

        assertThat(entity.createBy).isEqualTo(0L);
        assertThat(entity.updateBy).isEqualTo(0L);
    }

    @Test
    void updateFill_should_use_operator_id_when_context_is_populated() {
        OperatorContext.set(OPERATOR_ID, "test-operator");
        TestEntity entity = new TestEntity();
        MetaObject metaObject = metaObjectFor(entity);

        handler.updateFill(metaObject);

        assertThat(entity.updateBy).isEqualTo(OPERATOR_ID);
        assertThat(entity.updateTime).isNotNull();
    }

    @Test
    void updateFill_should_fallback_to_zero_when_operator_context_is_empty() {
        TestEntity entity = new TestEntity();
        MetaObject metaObject = metaObjectFor(entity);

        handler.updateFill(metaObject);

        assertThat(entity.updateBy).isEqualTo(0L);
    }

    @Test
    void updateFill_should_skip_update_by_when_no_setter() {
        NoFieldEntity entity = new NoFieldEntity();
        MetaObject metaObject = metaObjectFor(entity);

        handler.updateFill(metaObject);

        assertThat(entity.getClass().getDeclaredFields()).isEmpty();
    }

    private static MetaObject metaObjectFor(Object object) {
        return MetaObject.forObject(
                object,
                new DefaultObjectFactory(),
                new DefaultObjectWrapperFactory(),
                new DefaultReflectorFactory()
        );
    }

    @SuppressWarnings("unused")
    static class TestEntity {
        private Long createBy;
        private Long updateBy;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    @SuppressWarnings("unused")
    static class NoFieldEntity {
    }
}
