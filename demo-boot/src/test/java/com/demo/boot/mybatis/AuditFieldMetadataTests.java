package com.demo.boot.mybatis;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.demo.mdm.infra.entity.DictItemEntity;
import com.demo.mdm.infra.entity.DictTypeEntity;
import com.demo.mdm.infra.entity.GlobalDictItemEntity;
import com.demo.mdm.infra.entity.GlobalDictTypeEntity;
import com.demo.system.infra.entity.SysUserEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditFieldMetadataTests {

    @Test
    void audit_fields_should_declare_expected_fill_strategy() throws Exception {
        List<Class<?>> entityClasses = List.of(
                DictTypeEntity.class,
                DictItemEntity.class,
                GlobalDictTypeEntity.class,
                GlobalDictItemEntity.class,
                SysUserEntity.class
        );

        for (Class<?> entityClass : entityClasses) {
            assertFieldFill(entityClass, "createTime", FieldFill.INSERT);
            assertFieldFill(entityClass, "updateTime", FieldFill.INSERT_UPDATE);
            assertFieldFill(entityClass, "createBy", FieldFill.INSERT);
            assertFieldFill(entityClass, "updateBy", FieldFill.INSERT_UPDATE);
        }
    }

    private void assertFieldFill(Class<?> entityClass, String fieldName, FieldFill expectedFill) throws Exception {
        Field field = entityClass.getDeclaredField(fieldName);
        TableField tableField = field.getAnnotation(TableField.class);

        assertThat(tableField)
                .as("%s.%s should declare @TableField", entityClass.getSimpleName(), fieldName)
                .isNotNull();
        assertThat(tableField.fill())
                .as("%s.%s fill strategy", entityClass.getSimpleName(), fieldName)
                .isEqualTo(expectedFill);
    }
}
