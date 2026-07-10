package com.example.admin.core.mybatis;

import java.time.LocalDateTime;

import com.example.admin.core.operator.OperatorContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;

@Component
public class CommonMetaObjectHandler implements MetaObjectHandler {

    private static final Long FALLBACK_OPERATOR_ID = 0L;
    private static final String FIELD_UPDATE_TIME = "updateTime";
    private static final String FIELD_UPDATE_BY = "updateBy";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = resolveOperatorId();
        this.setFieldValByName("createTime", now, metaObject);
        this.setFieldValByName(FIELD_UPDATE_TIME, now, metaObject);
        this.setFieldValByName("createBy", operatorId, metaObject);
        this.setFieldValByName(FIELD_UPDATE_BY, operatorId, metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        if (metaObject.hasSetter(FIELD_UPDATE_TIME)) {
            metaObject.setValue(FIELD_UPDATE_TIME, LocalDateTime.now());
        }
        if (metaObject.hasSetter(FIELD_UPDATE_BY)) {
            metaObject.setValue(FIELD_UPDATE_BY, resolveOperatorId());
        }
    }

    private Long resolveOperatorId() {
        Long operatorId = OperatorContext.getOperatorId();
        return operatorId != null ? operatorId : FALLBACK_OPERATOR_ID;
    }
}
