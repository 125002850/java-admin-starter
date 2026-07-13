package com.example.admin.core.enums;

/**
 * 业务枚举基础接口。
 * 所有需要以对象形式序列化（code + desc）的业务枚举都应实现此接口，
 * 配合 {@link com.fasterxml.jackson.annotation.JsonFormat.Shape#OBJECT} 和
 * EnumModelConverter 实现 JSON 对象输出和 OpenAPI schema 映射。
 */
public interface BaseEnum {

    String getCode();

    String getDesc();
}
