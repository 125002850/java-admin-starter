package com.demo.core.mybatis;

import java.time.LocalDateTime;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;

@Component
public class CommonMetaObjectHandler implements MetaObjectHandler {

    private static final Long SYSTEM_USER_ID = 0L;
    private static final String FIELD_UPDATE_TIME = "updateTime";
    private static final String FIELD_UPDATE_BY = "updateBy";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, FIELD_UPDATE_TIME, LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createBy", Long.class, SYSTEM_USER_ID);
        this.strictInsertFill(metaObject, FIELD_UPDATE_BY, Long.class, SYSTEM_USER_ID);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        if (metaObject.hasSetter(FIELD_UPDATE_TIME)) {
            metaObject.setValue(FIELD_UPDATE_TIME, LocalDateTime.now());
        }
        if (metaObject.hasSetter(FIELD_UPDATE_BY)) {
            metaObject.setValue(FIELD_UPDATE_BY, SYSTEM_USER_ID);
        }
    }
}
