package com.oigit.admin.boot.mybatis;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.oigit.admin.mdm.dict.infra.entity.GlobalDictItemEntity;
import com.oigit.admin.mdm.dict.infra.entity.GlobalDictTypeEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditFieldMetadataTests {

    @Test
    void audit_fields_should_declare_expected_fill_strategy() throws Exception {
        List<Class<?>> entityClasses = List.of(
                GlobalDictTypeEntity.class,
                GlobalDictItemEntity.class
        );

        for (Class<?> entityClass : entityClasses) {
            assertFieldFill(entityClass, "createTime", FieldFill.INSERT);
            assertFieldFill(entityClass, "updateTime", FieldFill.INSERT_UPDATE);
            assertFieldFill(entityClass, "createBy", FieldFill.INSERT);
            assertFieldFill(entityClass, "updateBy", FieldFill.INSERT_UPDATE);
        }
    }

    private void assertFieldFill(Class<?> entityClass, String fieldName, FieldFill expectedFill) throws Exception {
        Field field = findFieldInHierarchy(entityClass, fieldName);
        assertThat(field)
                .as("%s.%s should be declared in class hierarchy", entityClass.getSimpleName(), fieldName)
                .isNotNull();
        TableField tableField = field.getAnnotation(TableField.class);

        assertThat(tableField)
                .as("%s.%s should declare @TableField", entityClass.getSimpleName(), fieldName)
                .isNotNull();
        assertThat(tableField.fill())
                .as("%s.%s fill strategy", entityClass.getSimpleName(), fieldName)
                .isEqualTo(expectedFill);
    }

    private Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
