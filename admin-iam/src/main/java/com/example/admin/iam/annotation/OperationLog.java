package com.example.admin.iam.annotation;

import com.example.admin.iam.enums.OperationLogAction;
import com.example.admin.iam.enums.OperationLogModule;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    OperationLogModule module();

    OperationLogAction action();
}
